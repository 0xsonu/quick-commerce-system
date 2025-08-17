package com.ecommerce.auth;

import com.ecommerce.auth.config.TokenCleanupProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
    "com.ecommerce.auth",
    "com.ecommerce.shared.models",
    "com.ecommerce.shared.utils",
    "com.ecommerce.shared.security"
})
@EnableScheduling
@EnableConfigurationProperties(TokenCleanupProperties.class)
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}