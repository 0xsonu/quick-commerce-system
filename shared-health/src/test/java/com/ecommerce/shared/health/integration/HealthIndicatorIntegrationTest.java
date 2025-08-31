package com.ecommerce.shared.health.integration;

import com.ecommerce.shared.health.indicator.DatabaseHealthIndicator;
import com.ecommerce.shared.health.indicator.MongoHealthIndicator;
import com.ecommerce.shared.health.indicator.RedisHealthIndicator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class HealthIndicatorIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static MongoDBContainer mongodb = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // MySQL configuration
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        // MongoDB configuration
        registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);

        // Redis configuration
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Test
    void shouldConnectToDatabaseSuccessfully() {
        // Given
        DataSource dataSource = createDataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        DatabaseHealthIndicator healthIndicator = new DatabaseHealthIndicator(dataSource, jdbcTemplate);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("connectionTime");
        assertThat(health.getDetails()).containsKey("queryTime");
    }

    @Test
    void shouldConnectToMongoSuccessfully() {
        // Given
        MongoTemplate mongoTemplate = createMongoTemplate();
        MongoHealthIndicator healthIndicator = new MongoHealthIndicator(mongoTemplate);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("connectionTime");
        assertThat(health.getDetails()).containsKey("operationTime");
        assertThat(health.getDetails()).containsKey("mongo.version");
    }

    @Test
    void shouldConnectToRedisSuccessfully() {
        // Given
        RedisConnectionFactory connectionFactory = createRedisConnectionFactory();
        RedisTemplate<String, Object> redisTemplate = createRedisTemplate(connectionFactory);
        RedisHealthIndicator healthIndicator = new RedisHealthIndicator(connectionFactory, redisTemplate);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("connectionTime");
        assertThat(health.getDetails()).containsKey("operationTime");
        assertThat(health.getDetails()).containsKey("redis.version");
    }

    private DataSource createDataSource() {
        org.springframework.boot.jdbc.DataSourceBuilder<?> builder = 
                org.springframework.boot.jdbc.DataSourceBuilder.create();
        return builder
                .url(mysql.getJdbcUrl())
                .username(mysql.getUsername())
                .password(mysql.getPassword())
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
    }

    private MongoTemplate createMongoTemplate() {
        com.mongodb.client.MongoClient mongoClient = 
                com.mongodb.client.MongoClients.create(mongodb.getReplicaSetUrl());
        return new MongoTemplate(mongoClient, "testdb");
    }

    private RedisConnectionFactory createRedisConnectionFactory() {
        org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory factory = 
                new org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory(
                        redis.getHost(), redis.getFirstMappedPort());
        factory.afterPropertiesSet();
        return factory;
    }

    private RedisTemplate<String, Object> createRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setDefaultSerializer(new org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}