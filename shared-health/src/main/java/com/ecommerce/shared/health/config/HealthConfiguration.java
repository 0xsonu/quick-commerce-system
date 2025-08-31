package com.ecommerce.shared.health.config;

import com.ecommerce.shared.health.indicator.*;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for health indicators
 */
@Configuration
public class HealthConfiguration {

    @Bean
    @ConditionalOnClass(DataSource.class)
    @ConditionalOnProperty(name = "management.health.db.enabled", matchIfMissing = true)
    public DatabaseHealthIndicator databaseHealthIndicator(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        return new DatabaseHealthIndicator(dataSource, jdbcTemplate);
    }

    @Bean
    @ConditionalOnClass(RedisConnectionFactory.class)
    @ConditionalOnProperty(name = "management.health.redis.enabled", matchIfMissing = true)
    public RedisHealthIndicator redisHealthIndicator(RedisConnectionFactory connectionFactory,
                                                   RedisTemplate<String, Object> redisTemplate) {
        return new RedisHealthIndicator(connectionFactory, redisTemplate);
    }

    @Bean
    @ConditionalOnClass(MongoTemplate.class)
    @ConditionalOnProperty(name = "management.health.mongo.enabled", matchIfMissing = true)
    public MongoHealthIndicator mongoHealthIndicator(MongoTemplate mongoTemplate) {
        return new MongoHealthIndicator(mongoTemplate);
    }

    @Bean
    @ConditionalOnClass(KafkaAdmin.class)
    @ConditionalOnProperty(name = "management.health.kafka.enabled", matchIfMissing = true)
    public KafkaHealthIndicator kafkaHealthIndicator(KafkaAdmin kafkaAdmin,
                                                   KafkaTemplate<String, Object> kafkaTemplate,
                                                   ProducerFactory<String, Object> producerFactory,
                                                   ConsumerFactory<String, Object> consumerFactory) {
        return new KafkaHealthIndicator(kafkaAdmin, kafkaTemplate, producerFactory, consumerFactory);
    }

    @Bean
    @ConditionalOnProperty(name = "management.health.jvm.enabled", matchIfMissing = true)
    public JvmHealthIndicator jvmHealthIndicator() {
        return new JvmHealthIndicator();
    }

    @Bean
    @ConditionalOnProperty(name = "management.health.composite.enabled", matchIfMissing = true)
    public CompositeHealthIndicator compositeHealthIndicator(Map<String, HealthIndicator> healthIndicators) {
        // Filter out the composite health indicator itself to avoid circular dependency
        Map<String, HealthIndicator> filteredIndicators = new HashMap<>();
        for (Map.Entry<String, HealthIndicator> entry : healthIndicators.entrySet()) {
            if (!(entry.getValue() instanceof CompositeHealthIndicator)) {
                filteredIndicators.put(entry.getKey(), entry.getValue());
            }
        }
        return new CompositeHealthIndicator(filteredIndicators);
    }
}