package com.ecommerce.inventoryservice.entity;

import com.ecommerce.shared.models.TenantAware;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_reservations",
       indexes = {
           @Index(name = "idx_tenant_order", columnList = "tenant_id, order_id", unique = true),
           @Index(name = "idx_tenant_reservation", columnList = "tenant_id, reservation_id", unique = true),
           @Index(name = "idx_tenant_inventory", columnList = "tenant_id, inventory_item_id"),
           @Index(name = "idx_tenant_status", columnList = "tenant_id, status"),
           @Index(name = "idx_expires_at", columnList = "expires_at")
       })
public class InventoryReservation implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tenant ID is required")
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @NotBlank(message = "Reservation ID is required")
    @Column(name = "reservation_id", nullable = false, length = 100)
    private String reservationId;

    @NotBlank(message = "Order ID is required")
    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    @NotNull(message = "Inventory item ID is required")
    @Column(name = "inventory_item_id", nullable = false)
    private Long inventoryItemId;

    @NotBlank(message = "Product ID is required")
    @Column(name = "product_id", nullable = false, length = 100)
    private String productId;

    @NotBlank(message = "SKU is required")
    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @NotNull(message = "Reserved quantity is required")
    @Positive(message = "Reserved quantity must be positive")
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Version
    @Column(name = "version")
    private Long version;

    // Constructors
    public InventoryReservation() {
        this.createdAt = LocalDateTime.now();
        this.status = ReservationStatus.ACTIVE;
    }

    public InventoryReservation(String tenantId, String reservationId, String orderId, 
                              Long inventoryItemId, String productId, String sku, 
                              Integer reservedQuantity, LocalDateTime expiresAt) {
        this();
        this.tenantId = tenantId;
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.inventoryItemId = inventoryItemId;
        this.productId = productId;
        this.sku = sku;
        this.reservedQuantity = reservedQuantity;
        this.expiresAt = expiresAt;
    }

    // Lifecycle callbacks
    @PreUpdate
    protected void onUpdate() {
        if (status == ReservationStatus.CONFIRMED && confirmedAt == null) {
            this.confirmedAt = LocalDateTime.now();
        } else if (status == ReservationStatus.RELEASED && releasedAt == null) {
            this.releasedAt = LocalDateTime.now();
        }
    }

    // Business methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt) && status == ReservationStatus.ACTIVE;
    }

    public boolean canBeConfirmed() {
        return status == ReservationStatus.ACTIVE && !isExpired();
    }

    public boolean canBeReleased() {
        return status == ReservationStatus.ACTIVE;
    }

    public void confirm() {
        if (!canBeConfirmed()) {
            throw new IllegalStateException("Reservation cannot be confirmed in current state: " + status);
        }
        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    public void release() {
        if (!canBeReleased()) {
            throw new IllegalStateException("Reservation cannot be released in current state: " + status);
        }
        this.status = ReservationStatus.RELEASED;
        this.releasedAt = LocalDateTime.now();
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

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Long getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(Long inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(LocalDateTime releasedAt) {
        this.releasedAt = releasedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public enum ReservationStatus {
        ACTIVE,     // Reservation is active and valid
        CONFIRMED,  // Reservation has been confirmed (stock deducted)
        RELEASED,   // Reservation has been released (stock returned)
        EXPIRED     // Reservation has expired
    }
}