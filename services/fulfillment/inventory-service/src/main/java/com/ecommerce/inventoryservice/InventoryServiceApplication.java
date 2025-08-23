package com.ecommerce.inventoryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
    "com.ecommerce.inventoryservice",
    "com.ecommerce.shared.security",
    "com.ecommerce.shared.utils"
})
@EntityScan(basePackages = {
    "com.ecommerce.inventoryservice.entity",
    "com.ecommerce.shared.models"
})
@EnableJpaRepositories(basePackages = {
    "com.ecommerce.inventoryservice.repository",
    "com.ecommerce.shared.utils.repository"
})
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}