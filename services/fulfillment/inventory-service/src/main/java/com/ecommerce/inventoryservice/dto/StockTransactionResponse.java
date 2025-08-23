package com.ecommerce.inventoryservice.dto;

import com.ecommerce.inventoryservice.entity.StockTransaction;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class StockTransactionResponse {

    private Long id;
    private Long inventoryItemId;
    private StockTransaction.TransactionType transactionType;
    private Integer quantity;
    private Integer previousAvailableQuantity;
    private Integer newAvailableQuantity;
    private Integer previousReservedQuantity;
    private Integer newReservedQuantity;
    private String referenceId;
    private String referenceType;
    private String reason;
    private String performedBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    // Constructors
    public StockTransactionResponse() {}

    public StockTransactionResponse(StockTransaction transaction) {
        this.id = transaction.getId();
        this.inventoryItemId = transaction.getInventoryItemId();
        this.transactionType = transaction.getTransactionType();
        this.quantity = transaction.getQuantity();
        this.previousAvailableQuantity = transaction.getPreviousAvailableQuantity();
        this.newAvailableQuantity = transaction.getNewAvailableQuantity();
        this.previousReservedQuantity = transaction.getPreviousReservedQuantity();
        this.newReservedQuantity = transaction.getNewReservedQuantity();
        this.referenceId = transaction.getReferenceId();
        this.referenceType = transaction.getReferenceType();
        this.reason = transaction.getReason();
        this.performedBy = transaction.getPerformedBy();
        this.createdAt = transaction.getCreatedAt();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(Long inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    public StockTransaction.TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(StockTransaction.TransactionType transactionType) {
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
}