package com.ecommerce.userservice.repository;

import com.ecommerce.userservice.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserAddress entity
 */
@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    /**
     * Find addresses by user ID
     */
    List<UserAddress> findByUserId(@Param("userId") Long userId);

    /**
     * Find addresses by user ID and type
     */
    List<UserAddress> findByUserIdAndType(@Param("userId") Long userId, 
                                         @Param("type") UserAddress.AddressType type);

    /**
     * Find default address by user ID and type
     */
    Optional<UserAddress> findByUserIdAndTypeAndIsDefaultTrue(@Param("userId") Long userId, 
                                                             @Param("type") UserAddress.AddressType type);

    /**
     * Find address by ID and user ID (for security)
     */
    Optional<UserAddress> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * Check if address exists by ID and user ID
     */
    boolean existsByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * Count addresses by user ID
     */
    long countByUserId(@Param("userId") Long userId);

    /**
     * Count addresses by user ID and type
     */
    long countByUserIdAndType(@Param("userId") Long userId, 
                             @Param("type") UserAddress.AddressType type);

    /**
     * Set all addresses of a type to non-default for a user
     */
    @Modifying
    @Query("UPDATE UserAddress ua SET ua.isDefault = false WHERE ua.user.id = :userId AND ua.type = :type")
    void setAllAddressesNonDefaultByUserIdAndType(@Param("userId") Long userId, 
                                                 @Param("type") UserAddress.AddressType type);

    /**
     * Delete addresses by user ID
     */
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * Delete address by ID and user ID (for security)
     */
    void deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}