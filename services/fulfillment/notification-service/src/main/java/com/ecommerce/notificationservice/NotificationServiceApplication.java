package com.ecommerce.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
    "com.ecommerce.notificationservice",
    "com.ecommerce.shared"
})
@EntityScan(basePackages = {
    "com.ecommerce.notificationservice.entity",
    "com.ecommerce.shared.models"
})
@EnableJpaRepositories(basePackages = {
    "com.ecommerce.notificationservice.repository"
})
@EnableKafka
@EnableAsync
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}