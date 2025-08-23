package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.dto.StockTransactionResponse;
import com.ecommerce.inventoryservice.entity.StockTransaction;
import com.ecommerce.inventoryservice.repository.StockTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockTransactionServiceTest {

    @Mock
    private StockTransactionRepository stockTransactionRepository;

    @InjectMocks
    private StockTransactionService stockTransactionService;

    private StockTransaction testTransaction;
    private final String tenantId = "tenant123";
    private final Long inventoryItemId = 1L;
    private final Long transactionId = 1L;

    @BeforeEach
    void setUp() {
        testTransaction = createTestTransaction();
        // Clear MDC before each test
        MDC.clear();
    }

    @Test
    void logTransaction_WithValidData_ShouldCreateTransaction() {
        // Given
        when(stockTransactionRepository.save(any(StockTransaction.class)))
            .thenReturn(testTransaction);

        // When
        StockTransactionResponse result = stockTransactionService.logTransaction(
            tenantId, inventoryItemId, StockTransaction.TransactionType.STOCK_IN,
            50, "REF123", 100, 150, 0, 0, "PURCHASE_ORDER", "Stock replenishment"
        );

        // Then
        assertNotNull(result);
        assertEquals(transactionId, result.getId());
        assertEquals(inventoryItemId, result.getInventoryItemId());
        assertEquals(StockTransaction.TransactionType.STOCK_IN, result.getTransactionType());
        assertEquals(50, result.getQuantity());
        assertEquals("REF123", result.getReferenceId());
        assertEquals("PURCHASE_ORDER", result.getReferenceType());
        assertEquals("Stock replenishment", result.getReason());
        
        verify(stockTransactionRepository).save(any(StockTransaction.class));
    }

    @Test
    void logTransaction_WithUserInMDC_ShouldSetPerformedBy() {
        // Given
        String userId = "user123";
        MDC.put("userId", userId);
        
        StockTransaction transactionWithUser = createTestTransaction();
        transactionWithUser.setPerformedBy(userId);
        
        when(stockTransactionRepository.save(any(StockTransaction.class)))
            .thenReturn(transactionWithUser);

        // When
        StockTransactionResponse result = stockTransactionService.logTransaction(
            tenantId, inventoryItemId, StockTransaction.TransactionType.STOCK_IN,
            50, "REF123", 100, 150, 0, 0, "PURCHASE_ORDER", "Stock replenishment"
        );

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getPerformedBy());
        
        verify(stockTransactionRepository).save(argThat((StockTransaction transaction) -> 
            userId.equals(transaction.getPerformedBy())));
    }

    @Test
    void getTransactionsByInventoryItem_ShouldReturnPagedTransactions() {
        // Given
        List<StockTransaction> transactions = Arrays.asList(testTransaction);
        Page<StockTransaction> page = new PageImpl<>(transactions, PageRequest.of(0, 20), 1);
        
        when(stockTransactionRepository.findByTenantIdAndInventoryItemId(
            eq(tenantId), eq(inventoryItemId), any(Pageable.class)))
            .thenReturn(page);

        // When
        Page<StockTransactionResponse> result = stockTransactionService
            .getTransactionsByInventoryItem(tenantId, inventoryItemId, 0, 20);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(transactionId, result.getContent().get(0).getId());
        
        verify(stockTransactionRepository).findByTenantIdAndInventoryItemId(
            eq(tenantId), eq(inventoryItemId), any(Pageable.class));
    }

    @Test
    void getTransactionsByType_ShouldReturnPagedTransactions() {
        // Given
        StockTransaction.TransactionType transactionType = StockTransaction.TransactionType.STOCK_IN;
        List<StockTransaction> transactions = Arrays.asList(testTransaction);
        Page<StockTransaction> page = new PageImpl<>(transactions, PageRequest.of(0, 20), 1);
        
        when(stockTransactionRepository.findByTenantIdAndTransactionType(
            eq(tenantId), eq(transactionType), any(Pageable.class)))
            .thenReturn(page);

        // When
        Page<StockTransactionResponse> result = stockTransactionService
            .getTransactionsByType(tenantId, transactionType, 0, 20);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(transactionType, result.getContent().get(0).getTransactionType());
        
        verify(stockTransactionRepository).findByTenantIdAndTransactionType(
            eq(tenantId), eq(transactionType), any(Pageable.class));
    }

    @Test
    void getTransactionsByReference_ShouldReturnTransactions() {
        // Given
        String referenceId = "REF123";
        List<StockTransaction> transactions = Arrays.asList(testTransaction);
        
        when(stockTransactionRepository.findByTenantIdAndReferenceId(tenantId, referenceId))
            .thenReturn(transactions);

        // When
        List<StockTransactionResponse> result = stockTransactionService
            .getTransactionsByReference(tenantId, referenceId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(referenceId, result.get(0).getReferenceId());
        
        verify(stockTransactionRepository).findByTenantIdAndReferenceId(tenantId, referenceId);
    }

    @Test
    void getTransactionsByDateRange_ShouldReturnPagedTransactions() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        List<StockTransaction> transactions = Arrays.asList(testTransaction);
        Page<StockTransaction> page = new PageImpl<>(transactions, PageRequest.of(0, 20), 1);
        
        when(stockTransactionRepository.findByTenantIdAndDateRange(
            eq(tenantId), eq(startDate), eq(endDate), any(Pageable.class)))
            .thenReturn(page);

        // When
        Page<StockTransactionResponse> result = stockTransactionService
            .getTransactionsByDateRange(tenantId, startDate, endDate, 0, 20);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        
        verify(stockTransactionRepository).findByTenantIdAndDateRange(
            eq(tenantId), eq(startDate), eq(endDate), any(Pageable.class));
    }

    @Test
    void getInventoryItemHistory_ShouldReturnTransactions() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        List<StockTransaction> transactions = Arrays.asList(testTransaction);
        
        when(stockTransactionRepository.findByInventoryItemAndDateRange(
            tenantId, inventoryItemId, startDate, endDate))
            .thenReturn(transactions);

        // When
        List<StockTransactionResponse> result = stockTransactionService
            .getInventoryItemHistory(tenantId, inventoryItemId, startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(inventoryItemId, result.get(0).getInventoryItemId());
        
        verify(stockTransactionRepository).findByInventoryItemAndDateRange(
            tenantId, inventoryItemId, startDate, endDate);
    }

    @Test
    void getTransactionSummary_ShouldReturnSummaryData() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        
        Object[] summaryData = {
            StockTransaction.TransactionType.STOCK_IN,
            5L,  // transaction count
            250L // total quantity
        };
        List<Object[]> summaryResults = new ArrayList<>();
        summaryResults.add(summaryData);
        
        when(stockTransactionRepository.getTransactionSummaryByType(tenantId, startDate, endDate))
            .thenReturn(summaryResults);

        // When
        List<StockTransactionService.TransactionSummary> result = stockTransactionService
            .getTransactionSummary(tenantId, startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        
        StockTransactionService.TransactionSummary summary = result.get(0);
        assertEquals(StockTransaction.TransactionType.STOCK_IN, summary.getTransactionType());
        assertEquals(5L, summary.getTransactionCount());
        assertEquals(250L, summary.getTotalQuantity());
        
        verify(stockTransactionRepository).getTransactionSummaryByType(tenantId, startDate, endDate);
    }

    @Test
    void getTransactionsByUser_ShouldReturnPagedTransactions() {
        // Given
        String performedBy = "user123";
        List<StockTransaction> transactions = Arrays.asList(testTransaction);
        Page<StockTransaction> page = new PageImpl<>(transactions, PageRequest.of(0, 20), 1);
        
        when(stockTransactionRepository.findByTenantIdAndPerformedBy(
            eq(tenantId), eq(performedBy), any(Pageable.class)))
            .thenReturn(page);

        // When
        Page<StockTransactionResponse> result = stockTransactionService
            .getTransactionsByUser(tenantId, performedBy, 0, 20);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        
        verify(stockTransactionRepository).findByTenantIdAndPerformedBy(
            eq(tenantId), eq(performedBy), any(Pageable.class));
    }

    @Test
    void getTransactionCountByType_ShouldReturnCount() {
        // Given
        StockTransaction.TransactionType transactionType = StockTransaction.TransactionType.STOCK_IN;
        when(stockTransactionRepository.countByTenantIdAndTransactionType(tenantId, transactionType))
            .thenReturn(10L);

        // When
        long result = stockTransactionService.getTransactionCountByType(tenantId, transactionType);

        // Then
        assertEquals(10L, result);
        verify(stockTransactionRepository).countByTenantIdAndTransactionType(tenantId, transactionType);
    }

    @Test
    void transactionSummary_ShouldHaveCorrectGetters() {
        // Given
        StockTransaction.TransactionType transactionType = StockTransaction.TransactionType.STOCK_OUT;
        Long transactionCount = 15L;
        Long totalQuantity = 500L;

        // When
        StockTransactionService.TransactionSummary summary = 
            new StockTransactionService.TransactionSummary(transactionType, transactionCount, totalQuantity);

        // Then
        assertEquals(transactionType, summary.getTransactionType());
        assertEquals(transactionCount, summary.getTransactionCount());
        assertEquals(totalQuantity, summary.getTotalQuantity());
    }

    // Helper methods
    private StockTransaction createTestTransaction() {
        StockTransaction transaction = new StockTransaction();
        transaction.setId(transactionId);
        transaction.setTenantId(tenantId);
        transaction.setInventoryItemId(inventoryItemId);
        transaction.setTransactionType(StockTransaction.TransactionType.STOCK_IN);
        transaction.setQuantity(50);
        transaction.setPreviousAvailableQuantity(100);
        transaction.setNewAvailableQuantity(150);
        transaction.setPreviousReservedQuantity(0);
        transaction.setNewReservedQuantity(0);
        transaction.setReferenceId("REF123");
        transaction.setReferenceType("PURCHASE_ORDER");
        transaction.setReason("Stock replenishment");
        transaction.setPerformedBy("user123");
        transaction.setCreatedAt(LocalDateTime.now());
        return transaction;
    }
}