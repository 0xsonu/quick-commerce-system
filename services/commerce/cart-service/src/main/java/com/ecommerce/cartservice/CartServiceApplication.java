package com.ecommerce.cartservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@SpringBootApplication(scanBasePackages = {
    "com.ecommerce.cartservice",
    "com.ecommerce.shared"
})
@EntityScan(basePackages = {
    "com.ecommerce.cartservice.entity",
    "com.ecommerce.shared.models"
})
@EnableJpaRepositories(basePackages = "com.ecommerce.cartservice.repository")
@EnableRedisRepositories(basePackages = "com.ecommerce.cartservice.redis")
@EnableCaching
public class CartServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CartServiceApplication.class, args);
    }
}