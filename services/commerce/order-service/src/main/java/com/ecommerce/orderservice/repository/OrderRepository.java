package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.shared.security.repository.TenantAwareRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends TenantAwareRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId AND o.userId = :userId")
    Page<Order> findByUserId(@Param("tenantId") String tenantId, 
                            @Param("userId") Long userId, 
                            Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId AND o.status = :status")
    List<Order> findByStatus(@Param("tenantId") String tenantId, 
                            @Param("status") OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId AND o.userId = :userId AND o.status = :status")
    List<Order> findByUserIdAndStatus(@Param("tenantId") String tenantId,
                                     @Param("userId") Long userId,
                                     @Param("status") OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.tenantId = :tenantId AND o.createdAt >= :startDate")
    Long countOrdersSince(@Param("tenantId") String tenantId, 
                         @Param("startDate") LocalDateTime startDate);

    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId AND o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findOrdersInDateRange(@Param("tenantId") String tenantId,
                                     @Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId AND o.createdAt BETWEEN :startDate AND :endDate")
    Page<Order> findOrdersByDateRange(@Param("tenantId") String tenantId,
                                     @Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate,
                                     Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId AND o.status IN :statuses")
    List<Order> findByStatusIn(@Param("tenantId") String tenantId, 
                              @Param("statuses") List<OrderStatus> statuses);

    boolean existsByOrderNumber(String orderNumber);
}