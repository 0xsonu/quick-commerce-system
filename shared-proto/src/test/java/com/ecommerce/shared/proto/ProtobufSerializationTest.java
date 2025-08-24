package com.ecommerce.shared.proto;

import com.ecommerce.productservice.proto.ProductServiceProtos.*;
import com.ecommerce.inventoryservice.proto.InventoryServiceProtos.*;
import com.ecommerce.userservice.proto.UserServiceProtos.*;
import com.ecommerce.shared.proto.CommonProtos.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test protobuf serialization and deserialization
 */
class ProtobufSerializationTest {

    @Test
    void testTenantContextSerialization() {
        // Create original object
        TenantContext original = TenantContext.newBuilder()
            .setTenantId("tenant123")
            .setUserId("user456")
            .setCorrelationId("corr789")
            .build();

        // Serialize to bytes
        byte[] serialized = original.toByteArray();
        assertNotNull(serialized);
        assertTrue(serialized.length > 0);

        // Deserialize from bytes
        try {
            TenantContext deserialized = TenantContext.parseFrom(serialized);
            
            // Verify all fields match
            assertEquals(original.getTenantId(), deserialized.getTenantId());
            assertEquals(original.getUserId(), deserialized.getUserId());
            assertEquals(original.getCorrelationId(), deserialized.getCorrelationId());
            assertEquals(original, deserialized);
        } catch (Exception e) {
            fail("Failed to deserialize TenantContext: " + e.getMessage());
        }
    }

    @Test
    void testMoneySerialization() {
        // Create original object
        Money original = Money.newBuilder()
            .setAmountCents(29999) // $299.99
            .setCurrency("USD")
            .build();

        // Serialize to bytes
        byte[] serialized = original.toByteArray();
        assertNotNull(serialized);

        // Deserialize from bytes
        try {
            Money deserialized = Money.parseFrom(serialized);
            
            // Verify all fields match
            assertEquals(original.getAmountCents(), deserialized.getAmountCents());
            assertEquals(original.getCurrency(), deserialized.getCurrency());
            assertEquals(original, deserialized);
        } catch (Exception e) {
            fail("Failed to deserialize Money: " + e.getMessage());
        }
    }

    @Test
    void testAddressSerialization() {
        // Create original object
        Address original = Address.newBuilder()
            .setStreetAddress("123 Main St")
            .setCity("New York")
            .setState("NY")
            .setPostalCode("10001")
            .setCountry("USA")
            .build();

        // Serialize to bytes
        byte[] serialized = original.toByteArray();
        assertNotNull(serialized);

        // Deserialize from bytes
        try {
            Address deserialized = Address.parseFrom(serialized);
            
            // Verify all fields match
            assertEquals(original.getStreetAddress(), deserialized.getStreetAddress());
            assertEquals(original.getCity(), deserialized.getCity());
            assertEquals(original.getState(), deserialized.getState());
            assertEquals(original.getPostalCode(), deserialized.getPostalCode());
            assertEquals(original.getCountry(), deserialized.getCountry());
            assertEquals(original, deserialized);
        } catch (Exception e) {
            fail("Failed to deserialize Address: " + e.getMessage());
        }
    }

    @Test
    void testProductValidationRequestSerialization() {
        // Create original object
        ValidateProductRequest original = ValidateProductRequest.newBuilder()
            .setContext(TenantContext.newBuilder()
                .setTenantId("tenant123")
                .setUserId("user456")
                .setCorrelationId("corr789")
                .build())
            .setProductId("prod123")
            .setSku("SKU123")
            .build();

        // Serialize to bytes
        byte[] serialized = original.toByteArray();
        assertNotNull(serialized);

        // Deserialize from bytes
        try {
            ValidateProductRequest deserialized = ValidateProductRequest.parseFrom(serialized);
            
            // Verify all fields match
            assertEquals(original.getContext(), deserialized.getContext());
            assertEquals(original.getProductId(), deserialized.getProductId());
            assertEquals(original.getSku(), deserialized.getSku());
            assertEquals(original, deserialized);
        } catch (Exception e) {
            fail("Failed to deserialize ValidateProductRequest: " + e.getMessage());
        }
    }

    @Test
    void testInventoryCheckRequestSerialization() {
        // Create original object
        CheckAvailabilityRequest original = CheckAvailabilityRequest.newBuilder()
            .setContext(TenantContext.newBuilder()
                .setTenantId("tenant123")
                .setUserId("user456")
                .build())
            .setProductId("prod123")
            .setRequestedQuantity(5)
            .build();

        // Serialize to bytes
        byte[] serialized = original.toByteArray();
        assertNotNull(serialized);

        // Deserialize from bytes
        try {
            CheckAvailabilityRequest deserialized = CheckAvailabilityRequest.parseFrom(serialized);
            
            // Verify all fields match
            assertEquals(original.getContext(), deserialized.getContext());
            assertEquals(original.getProductId(), deserialized.getProductId());
            assertEquals(original.getRequestedQuantity(), deserialized.getRequestedQuantity());
            assertEquals(original, deserialized);
        } catch (Exception e) {
            fail("Failed to deserialize CheckAvailabilityRequest: " + e.getMessage());
        }
    }

    @Test
    void testUserRequestSerialization() {
        // Create original object
        GetUserRequest original = GetUserRequest.newBuilder()
            .setContext(TenantContext.newBuilder()
                .setTenantId("tenant123")
                .setUserId("user456")
                .build())
            .setUserId(12345L)
            .build();

        // Serialize to bytes
        byte[] serialized = original.toByteArray();
        assertNotNull(serialized);

        // Deserialize from bytes
        try {
            GetUserRequest deserialized = GetUserRequest.parseFrom(serialized);
            
            // Verify all fields match
            assertEquals(original.getContext(), deserialized.getContext());
            assertEquals(original.getUserId(), deserialized.getUserId());
            assertEquals(original, deserialized);
        } catch (Exception e) {
            fail("Failed to deserialize GetUserRequest: " + e.getMessage());
        }
    }

    @Test
    void testComplexObjectWithNestedFields() {
        // Create a complex object with nested fields
        Product product = Product.newBuilder()
            .setId("prod123")
            .setName("Test Product")
            .setDescription("A test product")
            .setSku("SKU123")
            .setPrice(Money.newBuilder()
                .setAmountCents(29999)
                .setCurrency("USD")
                .build())
            .setCategory("Electronics")
            .setBrand("TestBrand")
            .setIsActive(true)
            .addImageUrls("https://example.com/image1.jpg")
            .addImageUrls("https://example.com/image2.jpg")
            .putAttributes("color", "black")
            .putAttributes("size", "medium")
            .build();

        GetProductResponse original = GetProductResponse.newBuilder()
            .setProduct(product)
            .build();

        // Serialize to bytes
        byte[] serialized = original.toByteArray();
        assertNotNull(serialized);

        // Deserialize from bytes
        try {
            GetProductResponse deserialized = GetProductResponse.parseFrom(serialized);
            
            // Verify complex nested object
            assertEquals(original.getProduct().getId(), deserialized.getProduct().getId());
            assertEquals(original.getProduct().getName(), deserialized.getProduct().getName());
            assertEquals(original.getProduct().getPrice().getAmountCents(), 
                        deserialized.getProduct().getPrice().getAmountCents());
            assertEquals(original.getProduct().getImageUrlsCount(), 
                        deserialized.getProduct().getImageUrlsCount());
            assertEquals(original.getProduct().getAttributesMap(), 
                        deserialized.getProduct().getAttributesMap());
            assertEquals(original, deserialized);
        } catch (Exception e) {
            fail("Failed to deserialize complex GetProductResponse: " + e.getMessage());
        }
    }

    @Test
    void testEmptyAndNullValues() {
        // Test with empty strings and default values
        TenantContext original = TenantContext.newBuilder()
            .setTenantId("")
            .setUserId("")
            .setCorrelationId("")
            .build();

        // Serialize to bytes
        byte[] serialized = original.toByteArray();
        assertNotNull(serialized);

        // Deserialize from bytes
        try {
            TenantContext deserialized = TenantContext.parseFrom(serialized);
            
            // Verify empty strings are preserved
            assertEquals("", deserialized.getTenantId());
            assertEquals("", deserialized.getUserId());
            assertEquals("", deserialized.getCorrelationId());
            assertEquals(original, deserialized);
        } catch (Exception e) {
            fail("Failed to deserialize TenantContext with empty values: " + e.getMessage());
        }
    }
}