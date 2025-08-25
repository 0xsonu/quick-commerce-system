package com.ecommerce.shippingservice.entity;

import com.ecommerce.shared.models.BaseEntity;
import com.ecommerce.shared.models.TenantAware;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "shipments", indexes = {
    @Index(name = "idx_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_order_id", columnList = "order_id"),
    @Index(name = "idx_tracking_number", columnList = "tracking_number"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_carrier_name", columnList = "carrier_name"),
    @Index(name = "idx_estimated_delivery", columnList = "estimated_delivery_date")
})
public class Shipment extends BaseEntity implements TenantAware {

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @NotNull
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @NotBlank
    @Column(name = "shipment_number", nullable = false, length = 100, unique = true)
    private String shipmentNumber;

    @NotBlank
    @Column(name = "carrier_name", nullable = false, length = 100)
    private String carrierName;

    @Column(name = "carrier_service", length = 100)
    private String carrierService;

    @Column(name = "tracking_number", length = 255)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShipmentStatus status = ShipmentStatus.CREATED;

    @Column(name = "shipping_address", columnDefinition = "JSON", nullable = false)
    private String shippingAddress; // JSON string

    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;

    @Column(name = "weight_kg", precision = 8, scale = 3)
    private BigDecimal weightKg;

    @Column(name = "dimensions_cm", columnDefinition = "JSON")
    private String dimensionsCm; // JSON string for {length, width, height}

    @Column(name = "shipping_cost", precision = 10, scale = 2)
    private BigDecimal shippingCost;

    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<ShipmentItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    @OrderBy("eventTime DESC")
    private List<ShipmentTracking> trackingEvents = new ArrayList<>();

    // Constructors
    public Shipment() {}

    public Shipment(String tenantId, Long orderId, String shipmentNumber, String carrierName) {
        this.tenantId = tenantId;
        this.orderId = orderId;
        this.shipmentNumber = shipmentNumber;
        this.carrierName = carrierName;
    }

    // Business methods
    public void updateStatus(ShipmentStatus newStatus) {
        this.status = newStatus;
        
        switch (newStatus) {
            case IN_TRANSIT:
                if (this.shippedAt == null) {
                    this.shippedAt = LocalDateTime.now();
                }
                break;
            case DELIVERED:
                if (this.deliveredAt == null) {
                    this.deliveredAt = LocalDateTime.now();
                    this.actualDeliveryDate = LocalDate.now();
                }
                break;
        }
    }

    public void addTrackingEvent(ShipmentTracking trackingEvent) {
        trackingEvents.add(trackingEvent);
        trackingEvent.setShipment(this);
    }

    public void addItem(ShipmentItem item) {
        items.add(item);
        item.setShipment(this);
    }

    public boolean isDelivered() {
        return status == ShipmentStatus.DELIVERED;
    }

    public boolean isInTransit() {
        return status == ShipmentStatus.IN_TRANSIT || status == ShipmentStatus.OUT_FOR_DELIVERY;
    }

    // Getters and Setters
    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getShipmentNumber() {
        return shipmentNumber;
    }

    public void setShipmentNumber(String shipmentNumber) {
        this.shipmentNumber = shipmentNumber;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    public String getCarrierService() {
        return carrierService;
    }

    public void setCarrierService(String carrierService) {
        this.carrierService = carrierService;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public LocalDate getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public LocalDate getActualDeliveryDate() {
        return actualDeliveryDate;
    }

    public void setActualDeliveryDate(LocalDate actualDeliveryDate) {
        this.actualDeliveryDate = actualDeliveryDate;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public String getDimensionsCm() {
        return dimensionsCm;
    }

    public void setDimensionsCm(String dimensionsCm) {
        this.dimensionsCm = dimensionsCm;
    }

    public BigDecimal getShippingCost() {
        return shippingCost;
    }

    public void setShippingCost(BigDecimal shippingCost) {
        this.shippingCost = shippingCost;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(LocalDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public List<ShipmentItem> getItems() {
        return items;
    }

    public void setItems(List<ShipmentItem> items) {
        this.items = items;
    }

    public List<ShipmentTracking> getTrackingEvents() {
        return trackingEvents;
    }

    public void setTrackingEvents(List<ShipmentTracking> trackingEvents) {
        this.trackingEvents = trackingEvents;
    }
}