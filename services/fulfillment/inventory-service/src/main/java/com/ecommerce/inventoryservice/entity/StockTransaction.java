package com.ecommerce.inventoryservice.entity;

import com.ecommerce.shared.models.TenantAware;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transactions",
       indexes = {
           @Index(name = "idx_tenant_inventory", columnList = "tenant_id, inventory_item_id"),
           @Index(name = "idx_tenant_type", columnList = "tenant_id, transaction_type"),
           @Index(name = "idx_tenant_reference", columnList = "tenant_id, reference_id"),
           @Index(name = "idx_created_at", columnList = "created_at")
       })
public class StockTransaction implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tenant ID is required")
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @NotNull(message = "Inventory item ID is required")
    @Column(name = "inventory_item_id", nullable = false)
    private Long inventoryItemId;

    @NotNull(message = "Transaction type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    @NotNull(message = "Quantity is required")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "previous_available_quantity")
    private Integer previousAvailableQuantity;

    @Column(name = "new_available_quantity")
    private Integer newAvailableQuantity;

    @Column(name = "previous_reserved_quantity")
    private Integer previousReservedQuantity;

    @Column(name = "new_reserved_quantity")
    private Integer newReservedQuantity;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "performed_by", length = 100)
    private String performedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public StockTransaction() {
        this.createdAt = LocalDateTime.now();
    }

    public StockTransaction(String tenantId, Long inventoryItemId, TransactionType transactionType,
                           Integer quantity, String referenceId, String referenceType, String reason) {
        this();
        this.tenantId = tenantId;
        this.inventoryItemId = inventoryItemId;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.reason = reason;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Long getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(Long inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getPreviousAvailableQuantity() {
        return previousAvailableQuantity;
    }

    public void setPreviousAvailableQuantity(Integer previousAvailableQuantity) {
        this.previousAvailableQuantity = previousAvailableQuantity;
    }

    public Integer getNewAvailableQuantity() {
        return newAvailableQuantity;
    }

    public void setNewAvailableQuantity(Integer newAvailableQuantity) {
        this.newAvailableQuantity = newAvailableQuantity;
    }

    public Integer getPreviousReservedQuantity() {
        return previousReservedQuantity;
    }

    public void setPreviousReservedQuantity(Integer previousReservedQuantity) {
        this.previousReservedQuantity = previousReservedQuantity;
    }

    public Integer getNewReservedQuantity() {
        return newReservedQuantity;
    }

    public void setNewReservedQuantity(Integer newReservedQuantity) {
        this.newReservedQuantity = newReservedQuantity;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public enum TransactionType {
        STOCK_IN,           // Adding stock (purchase, return, adjustment)
        STOCK_OUT,          // Removing stock (sale, damage, adjustment)
        RESERVATION,        // Reserving stock for order
        RESERVATION_RELEASE, // Releasing reserved stock
        RESERVATION_CONFIRM, // Confirming reserved stock (converting to sale)
        ADJUSTMENT_IN,      // Positive stock adjustment
        ADJUSTMENT_OUT,     // Negative stock adjustment
        TRANSFER_IN,        // Stock transfer in from another location
        TRANSFER_OUT        // Stock transfer out to another location
    }
}