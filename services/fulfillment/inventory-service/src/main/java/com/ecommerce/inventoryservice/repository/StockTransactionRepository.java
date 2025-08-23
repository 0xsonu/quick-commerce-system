package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.entity.StockTransaction;
import com.ecommerce.shared.utils.repository.TenantAwareRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockTransactionRepository extends TenantAwareRepository<StockTransaction, Long> {

    /**
     * Find all transactions for a specific inventory item
     */
    Page<StockTransaction> findByTenantIdAndInventoryItemId(String tenantId, Long inventoryItemId, Pageable pageable);

    /**
     * Find transactions by tenant ID and transaction type
     */
    Page<StockTransaction> findByTenantIdAndTransactionType(String tenantId, 
                                                          StockTransaction.TransactionType transactionType, 
                                                          Pageable pageable);

    /**
     * Find transactions by reference ID (e.g., order ID)
     */
    List<StockTransaction> findByTenantIdAndReferenceId(String tenantId, String referenceId);

    /**
     * Find transactions by reference type and reference ID
     */
    List<StockTransaction> findByTenantIdAndReferenceTypeAndReferenceId(String tenantId, 
                                                                       String referenceType, 
                                                                       String referenceId);

    /**
     * Find transactions within a date range
     */
    @Query("SELECT st FROM StockTransaction st WHERE st.tenantId = :tenantId " +
           "AND st.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY st.createdAt DESC")
    Page<StockTransaction> findByTenantIdAndDateRange(@Param("tenantId") String tenantId,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate,
                                                     Pageable pageable);

    /**
     * Find transactions for a specific inventory item within date range
     */
    @Query("SELECT st FROM StockTransaction st WHERE st.tenantId = :tenantId " +
           "AND st.inventoryItemId = :inventoryItemId " +
           "AND st.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY st.createdAt DESC")
    List<StockTransaction> findByInventoryItemAndDateRange(@Param("tenantId") String tenantId,
                                                          @Param("inventoryItemId") Long inventoryItemId,
                                                          @Param("startDate") LocalDateTime startDate,
                                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Get transaction summary by type for a date range
     */
    @Query("SELECT st.transactionType, COUNT(st), SUM(st.quantity) " +
           "FROM StockTransaction st WHERE st.tenantId = :tenantId " +
           "AND st.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY st.transactionType")
    List<Object[]> getTransactionSummaryByType(@Param("tenantId") String tenantId,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    /**
     * Find transactions performed by a specific user
     */
    Page<StockTransaction> findByTenantIdAndPerformedBy(String tenantId, String performedBy, Pageable pageable);

    /**
     * Count transactions by tenant
     */
    long countByTenantId(String tenantId);

    /**
     * Count transactions by tenant and type
     */
    long countByTenantIdAndTransactionType(String tenantId, StockTransaction.TransactionType transactionType);

    /**
     * Count transactions by tenant and inventory item
     */
    long countByTenantIdAndInventoryItemId(String tenantId, Long inventoryItemId);
}