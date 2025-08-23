package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.dto.StockTransactionResponse;
import com.ecommerce.inventoryservice.entity.StockTransaction;
import com.ecommerce.inventoryservice.repository.StockTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class StockTransactionService {

    private static final Logger logger = LoggerFactory.getLogger(StockTransactionService.class);

    private final StockTransactionRepository stockTransactionRepository;

    @Autowired
    public StockTransactionService(StockTransactionRepository stockTransactionRepository) {
        this.stockTransactionRepository = stockTransactionRepository;
    }

    /**
     * Log a stock transaction
     */
    public StockTransactionResponse logTransaction(String tenantId, Long inventoryItemId, 
                                                 StockTransaction.TransactionType transactionType,
                                                 Integer quantity, String referenceId,
                                                 Integer previousAvailable, Integer newAvailable,
                                                 Integer previousReserved, Integer newReserved,
                                                 String referenceType, String reason) {
        
        StockTransaction transaction = new StockTransaction();
        transaction.setTenantId(tenantId);
        transaction.setInventoryItemId(inventoryItemId);
        transaction.setTransactionType(transactionType);
        transaction.setQuantity(quantity);
        transaction.setPreviousAvailableQuantity(previousAvailable);
        transaction.setNewAvailableQuantity(newAvailable);
        transaction.setPreviousReservedQuantity(previousReserved);
        transaction.setNewReservedQuantity(newReserved);
        transaction.setReferenceId(referenceId);
        transaction.setReferenceType(referenceType);
        transaction.setReason(reason);
        
        // Get user from MDC if available
        String userId = MDC.get("userId");
        if (userId != null) {
            transaction.setPerformedBy(userId);
        }
        
        StockTransaction savedTransaction = stockTransactionRepository.save(transaction);
        
        logger.info("Logged stock transaction: type={}, quantity={}, inventoryItemId={}, tenantId={}", 
                   transactionType, quantity, inventoryItemId, tenantId);
        
        return new StockTransactionResponse(savedTransaction);
    }

    /**
     * Get transactions for a specific inventory item
     */
    @Transactional(readOnly = true)
    public Page<StockTransactionResponse> getTransactionsByInventoryItem(String tenantId, Long inventoryItemId, 
                                                                        int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<StockTransaction> transactionPage = stockTransactionRepository
            .findByTenantIdAndInventoryItemId(tenantId, inventoryItemId, pageable);
        
        return transactionPage.map(StockTransactionResponse::new);
    }

    /**
     * Get transactions by type
     */
    @Transactional(readOnly = true)
    public Page<StockTransactionResponse> getTransactionsByType(String tenantId, 
                                                              StockTransaction.TransactionType transactionType,
                                                              int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<StockTransaction> transactionPage = stockTransactionRepository
            .findByTenantIdAndTransactionType(tenantId, transactionType, pageable);
        
        return transactionPage.map(StockTransactionResponse::new);
    }

    /**
     * Get transactions by reference ID
     */
    @Transactional(readOnly = true)
    public List<StockTransactionResponse> getTransactionsByReference(String tenantId, String referenceId) {
        List<StockTransaction> transactions = stockTransactionRepository
            .findByTenantIdAndReferenceId(tenantId, referenceId);
        
        return transactions.stream()
            .map(StockTransactionResponse::new)
            .collect(Collectors.toList());
    }

    /**
     * Get transactions within date range
     */
    @Transactional(readOnly = true)
    public Page<StockTransactionResponse> getTransactionsByDateRange(String tenantId, 
                                                                   LocalDateTime startDate,
                                                                   LocalDateTime endDate,
                                                                   int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<StockTransaction> transactionPage = stockTransactionRepository
            .findByTenantIdAndDateRange(tenantId, startDate, endDate, pageable);
        
        return transactionPage.map(StockTransactionResponse::new);
    }

    /**
     * Get transaction history for inventory item within date range
     */
    @Transactional(readOnly = true)
    public List<StockTransactionResponse> getInventoryItemHistory(String tenantId, Long inventoryItemId,
                                                                LocalDateTime startDate, LocalDateTime endDate) {
        List<StockTransaction> transactions = stockTransactionRepository
            .findByInventoryItemAndDateRange(tenantId, inventoryItemId, startDate, endDate);
        
        return transactions.stream()
            .map(StockTransactionResponse::new)
            .collect(Collectors.toList());
    }

    /**
     * Get transaction summary by type for date range
     */
    @Transactional(readOnly = true)
    public List<TransactionSummary> getTransactionSummary(String tenantId, LocalDateTime startDate, 
                                                        LocalDateTime endDate) {
        List<Object[]> results = stockTransactionRepository
            .getTransactionSummaryByType(tenantId, startDate, endDate);
        
        return results.stream()
            .map(result -> new TransactionSummary(
                (StockTransaction.TransactionType) result[0],
                ((Number) result[1]).longValue(),
                ((Number) result[2]).longValue()
            ))
            .collect(Collectors.toList());
    }

    /**
     * Get transactions performed by user
     */
    @Transactional(readOnly = true)
    public Page<StockTransactionResponse> getTransactionsByUser(String tenantId, String performedBy, 
                                                              int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<StockTransaction> transactionPage = stockTransactionRepository
            .findByTenantIdAndPerformedBy(tenantId, performedBy, pageable);
        
        return transactionPage.map(StockTransactionResponse::new);
    }

    /**
     * Get transaction count by type
     */
    @Transactional(readOnly = true)
    public long getTransactionCountByType(String tenantId, StockTransaction.TransactionType transactionType) {
        return stockTransactionRepository.countByTenantIdAndTransactionType(tenantId, transactionType);
    }

    /**
     * Transaction summary DTO
     */
    public static class TransactionSummary {
        private final StockTransaction.TransactionType transactionType;
        private final Long transactionCount;
        private final Long totalQuantity;

        public TransactionSummary(StockTransaction.TransactionType transactionType, 
                                Long transactionCount, Long totalQuantity) {
            this.transactionType = transactionType;
            this.transactionCount = transactionCount;
            this.totalQuantity = totalQuantity;
        }

        public StockTransaction.TransactionType getTransactionType() {
            return transactionType;
        }

        public Long getTransactionCount() {
            return transactionCount;
        }

        public Long getTotalQuantity() {
            return totalQuantity;
        }
    }
}