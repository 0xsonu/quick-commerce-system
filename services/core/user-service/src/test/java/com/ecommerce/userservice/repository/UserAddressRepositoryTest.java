package com.ecommerce.userservice.repository;

import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.entity.UserAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserAddressRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserAddressRepository addressRepository;

    private User testUser;
    private UserAddress shippingAddress;
    private UserAddress billingAddress;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User("tenant1", 123L, "test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser = entityManager.persistAndFlush(testUser);

        // Create shipping address
        shippingAddress = new UserAddress(testUser, UserAddress.AddressType.SHIPPING,
                "123 Main St", "New York", "NY", "10001", "USA");
        shippingAddress.setIsDefault(true);
        shippingAddress = entityManager.persistAndFlush(shippingAddress);

        // Create billing address
        billingAddress = new UserAddress(testUser, UserAddress.AddressType.BILLING,
                "456 Oak Ave", "Los Angeles", "CA", "90210", "USA");
        billingAddress.setIsDefault(false);
        billingAddress = entityManager.persistAndFlush(billingAddress);

        entityManager.clear();
    }

    @Test
    void findByUserId_Success() {
        // Act
        List<UserAddress> addresses = addressRepository.findByUserId(testUser.getId());

        // Assert
        assertEquals(2, addresses.size());
        assertTrue(addresses.stream().anyMatch(addr -> addr.getType() == UserAddress.AddressType.SHIPPING));
        assertTrue(addresses.stream().anyMatch(addr -> addr.getType() == UserAddress.AddressType.BILLING));
    }

    @Test
    void findByUserIdAndType_Success() {
        // Act
        List<UserAddress> shippingAddresses = addressRepository.findByUserIdAndType(
                testUser.getId(), UserAddress.AddressType.SHIPPING);

        // Assert
        assertEquals(1, shippingAddresses.size());
        assertEquals(UserAddress.AddressType.SHIPPING, shippingAddresses.get(0).getType());
        assertEquals("123 Main St", shippingAddresses.get(0).getStreetAddress());
    }

    @Test
    void findByUserIdAndTypeAndIsDefaultTrue_Success() {
        // Act
        Optional<UserAddress> defaultShipping = addressRepository.findByUserIdAndTypeAndIsDefaultTrue(
                testUser.getId(), UserAddress.AddressType.SHIPPING);

        // Assert
        assertTrue(defaultShipping.isPresent());
        assertEquals(UserAddress.AddressType.SHIPPING, defaultShipping.get().getType());
        assertTrue(defaultShipping.get().getIsDefault());
    }

    @Test
    void findByUserIdAndTypeAndIsDefaultTrue_NotFound() {
        // Act
        Optional<UserAddress> defaultBilling = addressRepository.findByUserIdAndTypeAndIsDefaultTrue(
                testUser.getId(), UserAddress.AddressType.BILLING);

        // Assert
        assertFalse(defaultBilling.isPresent());
    }

    @Test
    void findByIdAndUserId_Success() {
        // Act
        Optional<UserAddress> address = addressRepository.findByIdAndUserId(
                shippingAddress.getId(), testUser.getId());

        // Assert
        assertTrue(address.isPresent());
        assertEquals(shippingAddress.getId(), address.get().getId());
        assertEquals(testUser.getId(), address.get().getUser().getId());
    }

    @Test
    void findByIdAndUserId_WrongUser() {
        // Arrange
        User anotherUser = new User("tenant1", 456L, "another@example.com");
        anotherUser = entityManager.persistAndFlush(anotherUser);

        // Act
        Optional<UserAddress> address = addressRepository.findByIdAndUserId(
                shippingAddress.getId(), anotherUser.getId());

        // Assert
        assertFalse(address.isPresent());
    }

    @Test
    void existsByIdAndUserId_Success() {
        // Act
        boolean exists = addressRepository.existsByIdAndUserId(
                shippingAddress.getId(), testUser.getId());

        // Assert
        assertTrue(exists);
    }

    @Test
    void existsByIdAndUserId_WrongUser() {
        // Arrange
        User anotherUser = new User("tenant1", 456L, "another@example.com");
        anotherUser = entityManager.persistAndFlush(anotherUser);

        // Act
        boolean exists = addressRepository.existsByIdAndUserId(
                shippingAddress.getId(), anotherUser.getId());

        // Assert
        assertFalse(exists);
    }

    @Test
    void countByUserId_Success() {
        // Act
        long count = addressRepository.countByUserId(testUser.getId());

        // Assert
        assertEquals(2, count);
    }

    @Test
    void countByUserIdAndType_Success() {
        // Act
        long shippingCount = addressRepository.countByUserIdAndType(
                testUser.getId(), UserAddress.AddressType.SHIPPING);
        long billingCount = addressRepository.countByUserIdAndType(
                testUser.getId(), UserAddress.AddressType.BILLING);

        // Assert
        assertEquals(1, shippingCount);
        assertEquals(1, billingCount);
    }

    @Test
    void setAllAddressesNonDefaultByUserIdAndType_Success() {
        // Arrange
        UserAddress anotherShipping = new UserAddress(testUser, UserAddress.AddressType.SHIPPING,
                "789 Pine St", "Chicago", "IL", "60601", "USA");
        anotherShipping.setIsDefault(true);
        entityManager.persistAndFlush(anotherShipping);

        // Act
        addressRepository.setAllAddressesNonDefaultByUserIdAndType(
                testUser.getId(), UserAddress.AddressType.SHIPPING);
        entityManager.flush();
        entityManager.clear();

        // Assert
        List<UserAddress> shippingAddresses = addressRepository.findByUserIdAndType(
                testUser.getId(), UserAddress.AddressType.SHIPPING);
        
        for (UserAddress address : shippingAddresses) {
            assertFalse(address.getIsDefault(), "Address should not be default: " + address.getId());
        }
    }

    @Test
    void deleteByIdAndUserId_Success() {
        // Arrange
        Long addressId = shippingAddress.getId();
        Long userId = testUser.getId();

        // Act
        addressRepository.deleteByIdAndUserId(addressId, userId);
        entityManager.flush();

        // Assert
        Optional<UserAddress> deletedAddress = addressRepository.findByIdAndUserId(addressId, userId);
        assertFalse(deletedAddress.isPresent());

        // Verify other address still exists
        Optional<UserAddress> otherAddress = addressRepository.findByIdAndUserId(billingAddress.getId(), userId);
        assertTrue(otherAddress.isPresent());
    }

    @Test
    void deleteByUserId_Success() {
        // Arrange
        Long userId = testUser.getId();

        // Act
        addressRepository.deleteByUserId(userId);
        entityManager.flush();

        // Assert
        List<UserAddress> addresses = addressRepository.findByUserId(userId);
        assertTrue(addresses.isEmpty());
    }

    @Test
    void cascadeDelete_WhenUserDeleted() {
        // Arrange
        Long userId = testUser.getId();
        Long addressId = shippingAddress.getId();

        // Act - Delete user (should cascade to addresses)
        entityManager.remove(entityManager.find(User.class, userId));
        entityManager.flush();

        // Assert
        Optional<UserAddress> address = addressRepository.findById(addressId);
        assertFalse(address.isPresent());
    }

    @Test
    void multipleDefaultAddresses_DifferentTypes() {
        // Arrange - Set billing address as default too
        billingAddress.setIsDefault(true);
        entityManager.persistAndFlush(billingAddress);

        // Act
        Optional<UserAddress> defaultShipping = addressRepository.findByUserIdAndTypeAndIsDefaultTrue(
                testUser.getId(), UserAddress.AddressType.SHIPPING);
        Optional<UserAddress> defaultBilling = addressRepository.findByUserIdAndTypeAndIsDefaultTrue(
                testUser.getId(), UserAddress.AddressType.BILLING);

        // Assert - Should be able to have default addresses for different types
        assertTrue(defaultShipping.isPresent());
        assertTrue(defaultBilling.isPresent());
        assertEquals(UserAddress.AddressType.SHIPPING, defaultShipping.get().getType());
        assertEquals(UserAddress.AddressType.BILLING, defaultBilling.get().getType());
    }
}