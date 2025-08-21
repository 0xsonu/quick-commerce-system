package com.ecommerce.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {
    "com.ecommerce.userservice",
    "com.ecommerce.shared.security",
    "com.ecommerce.shared.utils"
})
@EntityScan(basePackages = {
    "com.ecommerce.userservice.entity",
    "com.ecommerce.shared.models"
})
@EnableCaching
@EnableTransactionManagement
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}