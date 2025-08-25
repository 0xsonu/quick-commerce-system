package com.ecommerce.shippingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
    "com.ecommerce.shippingservice",
    "com.ecommerce.shared"
})
@EntityScan(basePackages = {
    "com.ecommerce.shippingservice.entity",
    "com.ecommerce.shared.models"
})
@EnableJpaRepositories(
    basePackages = "com.ecommerce.shippingservice.repository",
    repositoryFactoryBeanClass = com.ecommerce.shared.security.repository.TenantAwareRepositoryFactoryBean.class
)
@EnableKafka
@EnableAsync
@EnableScheduling
public class ShippingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShippingServiceApplication.class, args);
    }
}