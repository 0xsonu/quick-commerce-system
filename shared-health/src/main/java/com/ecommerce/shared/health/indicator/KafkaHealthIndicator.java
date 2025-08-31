package com.ecommerce.shared.health.indicator;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Health indicator for Kafka messaging system
 */
@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(KafkaHealthIndicator.class);
    
    private final KafkaAdmin kafkaAdmin;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ProducerFactory<String, Object> producerFactory;
    private final ConsumerFactory<String, Object> consumerFactory;
    
    // Health check thresholds
    private static final long SLOW_OPERATION_THRESHOLD_MS = 2000; // 2 seconds
    private static final String HEALTH_CHECK_TOPIC = "health-check-topic";
    private static final int TIMEOUT_SECONDS = 5;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin,
                               KafkaTemplate<String, Object> kafkaTemplate,
                               ProducerFactory<String, Object> producerFactory,
                               ConsumerFactory<String, Object> consumerFactory) {
        this.kafkaAdmin = kafkaAdmin;
        this.kafkaTemplate = kafkaTemplate;
        this.producerFactory = producerFactory;
        this.consumerFactory = consumerFactory;
    }

    @Override
    public Health health() {
        try {
            return checkKafkaHealth();
        } catch (Exception e) {
            logger.error("Kafka health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("timestamp", Instant.now().toString())
                    .build();
        }
    }

    private Health checkKafkaHealth() {
        Health.Builder healthBuilder = Health.up();
        
        // Check cluster connectivity
        Instant start = Instant.now();
        boolean isConnected = checkClusterConnection();
        Duration connectionTime = Duration.between(start, Instant.now());
        
        if (!isConnected) {
            return Health.down()
                    .withDetail("error", "Unable to connect to Kafka cluster")
                    .withDetail("connectionTime", connectionTime.toMillis() + "ms")
                    .withDetail("timestamp", Instant.now().toString())
                    .build();
        }
        
        healthBuilder.withDetail("connectionTime", connectionTime.toMillis() + "ms");
        
        // Check producer functionality
        start = Instant.now();
        boolean producerHealthy = checkProducer();
        Duration producerTime = Duration.between(start, Instant.now());
        
        healthBuilder.withDetail("producerTime", producerTime.toMillis() + "ms");
        
        if (!producerHealthy) {
            healthBuilder.status("DEGRADED")
                    .withDetail("warning", "Kafka producer is slow or failing");
        }
        
        // Check consumer functionality
        start = Instant.now();
        boolean consumerHealthy = checkConsumer();
        Duration consumerTime = Duration.between(start, Instant.now());
        
        healthBuilder.withDetail("consumerTime", consumerTime.toMillis() + "ms");
        
        if (!consumerHealthy) {
            healthBuilder.status("DEGRADED")
                    .withDetail("warning", "Kafka consumer is slow or failing");
        }
        
        // Add cluster information
        addClusterInfo(healthBuilder);
        
        // Determine overall status
        if (connectionTime.toMillis() > SLOW_OPERATION_THRESHOLD_MS || 
            producerTime.toMillis() > SLOW_OPERATION_THRESHOLD_MS ||
            consumerTime.toMillis() > SLOW_OPERATION_THRESHOLD_MS) {
            healthBuilder.status("DEGRADED");
        }
        
        healthBuilder.withDetail("timestamp", Instant.now().toString());
        
        return healthBuilder.build();
    }

    private boolean checkClusterConnection() {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            DescribeClusterResult clusterResult = adminClient.describeCluster();
            clusterResult.clusterId().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            logger.warn("Kafka cluster connection check failed", e);
            return false;
        }
    }

    private boolean checkProducer() {
        try (Producer<String, Object> producer = producerFactory.createProducer()) {
            String testMessage = "health-check-" + System.currentTimeMillis();
            ProducerRecord<String, Object> record = new ProducerRecord<>(HEALTH_CHECK_TOPIC, testMessage);
            
            Future<RecordMetadata> future = producer.send(record);
            RecordMetadata metadata = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            return metadata != null;
        } catch (Exception e) {
            logger.warn("Kafka producer check failed", e);
            return false;
        }
    }

    private boolean checkConsumer() {
        try (Consumer<String, Object> consumer = consumerFactory.createConsumer()) {
            // Just check if we can create a consumer and get metadata
            consumer.listTopics(Duration.ofSeconds(TIMEOUT_SECONDS));
            return true;
        } catch (Exception e) {
            logger.warn("Kafka consumer check failed", e);
            return false;
        }
    }

    private void addClusterInfo(Health.Builder healthBuilder) {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            DescribeClusterResult clusterResult = adminClient.describeCluster();
            
            String clusterId = clusterResult.clusterId().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            int nodeCount = clusterResult.nodes().get(TIMEOUT_SECONDS, TimeUnit.SECONDS).size();
            
            healthBuilder.withDetail("cluster.id", clusterId)
                    .withDetail("cluster.nodes", nodeCount);
            
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.debug("Could not retrieve Kafka cluster information", e);
        }
    }
}