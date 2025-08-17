package com.ecommerce.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = "com.ecommerce")
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r.path("/api/v1/auth/**")
                        .uri("http://localhost:8082"))
                .route("user-service", r -> r.path("/api/v1/users/**")
                        .uri("http://localhost:8083"))
                .route("product-service", r -> r.path("/api/v1/products/**")
                        .uri("http://localhost:8084"))
                .route("inventory-service", r -> r.path("/api/v1/inventory/**")
                        .uri("http://localhost:8085"))
                .route("cart-service", r -> r.path("/api/v1/cart/**")
                        .uri("http://localhost:8086"))
                .route("order-service", r -> r.path("/api/v1/orders/**")
                        .uri("http://localhost:8087"))
                .route("payment-service", r -> r.path("/api/v1/payments/**")
                        .uri("http://localhost:8088"))
                .route("shipping-service", r -> r.path("/api/v1/shipping/**")
                        .uri("http://localhost:8089"))
                .route("notification-service", r -> r.path("/api/v1/notifications/**")
                        .uri("http://localhost:8090"))
                .route("review-service", r -> r.path("/api/v1/reviews/**")
                        .uri("http://localhost:8091"))
                .build();
    }
}