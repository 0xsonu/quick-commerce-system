package com.ecommerce.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {
    "com.ecommerce.orderservice",
    "com.ecommerce.shared.utils",
    "com.ecommerce.shared.security"
})
@EntityScan(basePackages = {
    "com.ecommerce.orderservice.entity",
    "com.ecommerce.shared.models"
})
@EnableJpaRepositories(
    basePackages = "com.ecommerce.orderservice.repository",
    repositoryFactoryBeanClass = com.ecommerce.shared.security.repository.TenantAwareRepositoryFactoryBean.class
)
@EnableKafka
@EnableTransactionManagement
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}