package com.ecommerce.userservice.repository;

import com.ecommerce.userservice.entity.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for UserPreferences entity
 */
@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {

    /**
     * Find preferences by user ID
     */
    Optional<UserPreferences> findByUserId(@Param("userId") Long userId);

    /**
     * Check if preferences exist by user ID
     */
    boolean existsByUserId(@Param("userId") Long userId);

    /**
     * Delete preferences by user ID
     */
    void deleteByUserId(@Param("userId") Long userId);
}