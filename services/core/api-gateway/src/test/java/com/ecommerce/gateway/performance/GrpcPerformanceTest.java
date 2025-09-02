package com.ecommerce.gateway.performance;

import com.ecommerce.gateway.grpc.GrpcMessageMapper;
import com.ecommerce.gateway.service.GatewayGrpcClientService;
import com.ecommerce.shared.proto.CommonProtos;
import com.ecommerce.userservice.proto.UserServiceProtos;
import com.ecommerce.productservice.proto.ProductServiceProtos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * Performance tests for gRPC communication in API Gateway
 * These tests verify that gRPC calls meet performance requirements
 * 
 * Run with: -Dperformance.tests.enabled=true
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "performance.tests.enabled", matches = "true")
class GrpcPerformanceTest {

    private final GrpcMessageMapper messageMapper = new GrpcMessageMapper();

    @Test
    void testMessageMappingPerformance() {
        // Test the performance of message mapping operations
        int iterations = 10000;
        
        // Create test data
        CommonProtos.Money price = CommonProtos.Money.newBuilder()
            .setAmountCents(2999)
            .setCurrency("USD")
            .build();

        ProductServiceProtos.Product product = ProductServiceProtos.Product.newBuilder()
            .setId("product-123")
            .setName("Test Product")
            .setDescription("A test product for performance testing")
            .setSku("SKU-123")
            .setPrice(price)
            .setCategory("Electronics")
            .setBrand("TestBrand")
            .setIsActive(true)
            .addImageUrls("https://example.com/image1.jpg")
            .putAttributes("color", "blue")
            .build();

        ProductServiceProtos.GetProductResponse response = ProductServiceProtos.GetProductResponse.newBuilder()
            .setProduct(product)
            .build();

        // Warm up
        for (int i = 0; i < 1000; i++) {
            messageMapper.mapToProductResponse(response);
        }

        // Measure performance
        Instant start = Instant.now();
        
        for (int i = 0; i < iterations; i++) {
            GrpcMessageMapper.ProductResponse mappedResponse = messageMapper.mapToProductResponse(response);
            assertThat(mappedResponse.getId()).isEqualTo("product-123");
        }
        
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        
        // Verify performance requirements
        long avgMicrosPerMapping = duration.toNanos() / iterations / 1000;
        System.out.printf("Message mapping performance: %d iterations in %d ms (avg: %d μs per mapping)%n", 
            iterations, duration.toMillis(), avgMicrosPerMapping);
        
        // Assert that mapping takes less than 100 microseconds on average
        assertThat(avgMicrosPerMapping).isLessThan(100);
    }

    @Test
    void testMoneyConversionPerformance() {
        int iterations = 100000;
        
        CommonProtos.Money grpcMoney = CommonProtos.Money.newBuilder()
            .setAmountCents(12345)
            .setCurrency("USD")
            .build();

        // Warm up
        for (int i = 0; i < 10000; i++) {
            messageMapper.mapMoney(grpcMoney);
        }

        // Measure performance
        Instant start = Instant.now();
        
        for (int i = 0; i < iterations; i++) {
            BigDecimal result = messageMapper.mapMoney(grpcMoney);
            assertThat(result).isEqualTo(new BigDecimal("123.45"));
        }
        
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        
        long avgNanosPerConversion = duration.toNanos() / iterations;
        System.out.printf("Money conversion performance: %d iterations in %d ms (avg: %d ns per conversion)%n", 
            iterations, duration.toMillis(), avgNanosPerConversion);
        
        // Assert that money conversion takes less than 1 microsecond on average
        assertThat(avgNanosPerConversion).isLessThan(1000);
    }

    @Test
    void testConcurrentMessageMapping() throws Exception {
        int threadCount = 10;
        int iterationsPerThread = 1000;
        
        // Create test data
        UserServiceProtos.User user = UserServiceProtos.User.newBuilder()
            .setId(123L)
            .setFirstName("John")
            .setLastName("Doe")
            .setEmail("john.doe@example.com")
            .setIsActive(true)
            .build();

        UserServiceProtos.GetUserResponse response = UserServiceProtos.GetUserResponse.newBuilder()
            .setUser(user)
            .build();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        try {
            Instant start = Instant.now();
            
            List<CompletableFuture<Void>> futures = IntStream.range(0, threadCount)
                .mapToObj(threadIndex -> CompletableFuture.runAsync(() -> {
                    for (int i = 0; i < iterationsPerThread; i++) {
                        GrpcMessageMapper.UserProfileResponse mappedResponse = messageMapper.mapToUserProfileResponse(response);
                        assertThat(mappedResponse.getId()).isEqualTo(123L);
                        assertThat(mappedResponse.getFirstName()).isEqualTo("John");
                    }
                }, executor))
                .toList();

            // Wait for all threads to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
            
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            
            int totalOperations = threadCount * iterationsPerThread;
            long avgMicrosPerMapping = duration.toNanos() / totalOperations / 1000;
            
            System.out.printf("Concurrent mapping performance: %d operations across %d threads in %d ms (avg: %d μs per mapping)%n", 
                totalOperations, threadCount, duration.toMillis(), avgMicrosPerMapping);
            
            // Assert that concurrent mapping maintains good performance
            assertThat(avgMicrosPerMapping).isLessThan(200); // Allow for some overhead in concurrent execution
            
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void testTenantContextBuildingPerformance() {
        int iterations = 50000;
        
        String tenantId = "tenant-123";
        String userId = "user-456";
        String correlationId = "corr-789";

        // Warm up
        for (int i = 0; i < 5000; i++) {
            messageMapper.buildTenantContext(tenantId, userId, correlationId);
        }

        // Measure performance
        Instant start = Instant.now();
        
        for (int i = 0; i < iterations; i++) {
            CommonProtos.TenantContext context = messageMapper.buildTenantContext(tenantId, userId, correlationId);
            assertThat(context.getTenantId()).isEqualTo(tenantId);
        }
        
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        
        long avgNanosPerBuild = duration.toNanos() / iterations;
        System.out.printf("Tenant context building performance: %d iterations in %d ms (avg: %d ns per build)%n", 
            iterations, duration.toMillis(), avgNanosPerBuild);
        
        // Assert that context building is very fast
        assertThat(avgNanosPerBuild).isLessThan(2000); // Less than 2 microseconds
    }

    @Test
    void testLargePayloadMapping() {
        // Test performance with large payloads (e.g., many products)
        int productCount = 1000;
        
        List<ProductServiceProtos.Product> products = new ArrayList<>();
        for (int i = 0; i < productCount; i++) {
            CommonProtos.Money price = CommonProtos.Money.newBuilder()
                .setAmountCents(1999 + i)
                .setCurrency("USD")
                .build();

            ProductServiceProtos.Product product = ProductServiceProtos.Product.newBuilder()
                .setId("product-" + i)
                .setName("Product " + i)
                .setDescription("Description for product " + i)
                .setSku("SKU-" + i)
                .setPrice(price)
                .setCategory("Category-" + (i % 10))
                .setBrand("Brand-" + (i % 5))
                .setIsActive(true)
                .addImageUrls("https://example.com/image" + i + ".jpg")
                .putAttributes("color", "color-" + (i % 3))
                .build();
            
            products.add(product);
        }

        ProductServiceProtos.GetProductsByIdsResponse response = ProductServiceProtos.GetProductsByIdsResponse.newBuilder()
            .addAllProducts(products)
            .build();

        // Warm up
        for (int i = 0; i < 10; i++) {
            messageMapper.mapToProductsResponse(response);
        }

        // Measure performance
        int iterations = 100;
        Instant start = Instant.now();
        
        for (int i = 0; i < iterations; i++) {
            List<GrpcMessageMapper.ProductResponse> mappedProducts = messageMapper.mapToProductsResponse(response);
            assertThat(mappedProducts).hasSize(productCount);
        }
        
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        
        long avgMillisPerMapping = duration.toMillis() / iterations;
        System.out.printf("Large payload mapping performance: %d products x %d iterations in %d ms (avg: %d ms per mapping)%n", 
            productCount, iterations, duration.toMillis(), avgMillisPerMapping);
        
        // Assert that large payload mapping completes within reasonable time
        assertThat(avgMillisPerMapping).isLessThan(50); // Less than 50ms for 1000 products
    }

    @Test
    void testMemoryUsageStability() {
        // Test that repeated operations don't cause memory leaks
        int iterations = 10000;
        
        CommonProtos.Money price = CommonProtos.Money.newBuilder()
            .setAmountCents(2999)
            .setCurrency("USD")
            .build();

        ProductServiceProtos.Product product = ProductServiceProtos.Product.newBuilder()
            .setId("product-123")
            .setName("Test Product")
            .setPrice(price)
            .setIsActive(true)
            .build();

        ProductServiceProtos.GetProductResponse response = ProductServiceProtos.GetProductResponse.newBuilder()
            .setProduct(product)
            .build();

        // Force garbage collection before test
        System.gc();
        long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Perform many operations
        for (int i = 0; i < iterations; i++) {
            GrpcMessageMapper.ProductResponse mappedResponse = messageMapper.mapToProductResponse(response);
            // Don't hold references to prevent accumulation
        }

        // Force garbage collection after test
        System.gc();
        long finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        long memoryIncrease = finalMemory - initialMemory;
        System.out.printf("Memory usage: Initial: %d KB, Final: %d KB, Increase: %d KB%n", 
            initialMemory / 1024, finalMemory / 1024, memoryIncrease / 1024);
        
        // Assert that memory increase is reasonable (less than 10MB for 10k operations)
        assertThat(memoryIncrease).isLessThan(10 * 1024 * 1024);
    }
}