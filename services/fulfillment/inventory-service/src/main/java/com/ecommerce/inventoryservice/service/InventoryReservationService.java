package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.dto.ReservationRequest;
import com.ecommerce.inventoryservice.dto.ReservationResponse;
import com.ecommerce.inventoryservice.entity.InventoryItem;
import com.ecommerce.inventoryservice.entity.InventoryReservation;
import com.ecommerce.inventoryservice.entity.StockTransaction;
import com.ecommerce.inventoryservice.repository.InventoryItemRepository;
import com.ecommerce.inventoryservice.repository.InventoryReservationRepository;
import com.ecommerce.shared.models.events.InventoryReservedEvent;
import com.ecommerce.shared.models.events.InventoryReservationFailedEvent;
import com.ecommerce.shared.models.events.InventoryReleasedEvent;
import com.ecommerce.shared.utils.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryReservationService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryReservationService.class);
    private static final String RESERVATION_CACHE_PREFIX = "reservation:";
    private static final String INVENTORY_LOCK_PREFIX = "inventory_lock:";

    private final InventoryReservationRepository reservationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockTransactionService stockTransactionService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.inventory.reservation.ttl-minutes:30}")
    private int reservationTtlMinutes;

    @Value("${app.kafka.topics.inventory-events:inventory-events}")
    private String inventoryEventsTopic;

    @Autowired
    public InventoryReservationService(InventoryReservationRepository reservationRepository,
                                     InventoryItemRepository inventoryItemRepository,
                                     StockTransactionService stockTransactionService,
                                     RedisTemplate<String, Object> redisTemplate,
                                     KafkaTemplate<String, Object> kafkaTemplate) {
        this.reservationRepository = reservationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.stockTransactionService = stockTransactionService;
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Reserve inventory for an order
     */
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public ReservationResponse reserveInventory(String tenantId, ReservationRequest request) {
        String reservationId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(reservationTtlMinutes);
        
        // Check if reservation already exists for this order
        if (reservationRepository.existsByTenantIdAndOrderId(tenantId, request.getOrderId())) {
            throw new IllegalArgumentException("Reservation already exists for order: " + request.getOrderId());
        }

        List<InventoryReservation> reservations = new ArrayList<>();
        List<InventoryReservedEvent.ReservedItemData> reservedItems = new ArrayList<>();
        List<InventoryReservationFailedEvent.FailedItemData> failedItems = new ArrayList<>();

        try {
            // Process each item in the reservation request
            for (ReservationRequest.ReservationItemRequest itemRequest : request.getItems()) {
                try {
                    InventoryReservation reservation = processItemReservation(
                        tenantId, reservationId, request.getOrderId(), itemRequest, expiresAt);
                    
                    if (reservation != null) {
                        reservations.add(reservation);
                        reservedItems.add(new InventoryReservedEvent.ReservedItemData(
                            reservation.getProductId(),
                            reservation.getSku(),
                            reservation.getReservedQuantity(),
                            reservation.getInventoryItemId()
                        ));
                    }
                } catch (Exception e) {
                    logger.warn("Failed to reserve item: productId={}, sku={}, quantity={}, error={}", 
                               itemRequest.getProductId(), itemRequest.getSku(), 
                               itemRequest.getQuantity(), e.getMessage());
                    
                    // Find available quantity for failed item
                    Integer availableQuantity = 0;
                    try {
                        InventoryItem item = inventoryItemRepository
                            .findByTenantIdAndProductId(tenantId, itemRequest.getProductId())
                            .orElse(null);
                        if (item != null) {
                            availableQuantity = item.getAvailableQuantity();
                        }
                    } catch (Exception ignored) {}

                    failedItems.add(new InventoryReservationFailedEvent.FailedItemData(
                        itemRequest.getProductId(),
                        itemRequest.getSku(),
                        itemRequest.getQuantity(),
                        availableQuantity,
                        e.getMessage()
                    ));
                }
            }

            // If any items failed, rollback all reservations
            if (!failedItems.isEmpty()) {
                rollbackReservations(tenantId, reservations);
                
                // Publish failure event
                InventoryReservationFailedEvent failureEvent = new InventoryReservationFailedEvent(
                    tenantId, request.getOrderId(), failedItems, "Insufficient inventory for some items");
                kafkaTemplate.send(inventoryEventsTopic, failureEvent);
                
                return ReservationResponse.failed(reservationId, failedItems);
            }

            // Cache reservation data in Redis
            cacheReservationData(tenantId, reservationId, reservations);

            // Publish success event
            InventoryReservedEvent successEvent = new InventoryReservedEvent(
                tenantId, request.getOrderId(), reservationId, reservedItems);
            kafkaTemplate.send(inventoryEventsTopic, successEvent);

            logger.info("Successfully reserved inventory for order: {}, reservationId: {}, items: {}", 
                       request.getOrderId(), reservationId, reservedItems.size());

            return ReservationResponse.success(reservationId, reservedItems);

        } catch (Exception e) {
            logger.error("Failed to reserve inventory for order: {}", request.getOrderId(), e);
            rollbackReservations(tenantId, reservations);
            throw e;
        }
    }

    /**
     * Process reservation for a single item
     */
    private InventoryReservation processItemReservation(String tenantId, String reservationId, 
                                                       String orderId, ReservationRequest.ReservationItemRequest itemRequest,
                                                       LocalDateTime expiresAt) {
        // Find inventory item
        InventoryItem inventoryItem = inventoryItemRepository
            .findByTenantIdAndProductId(tenantId, itemRequest.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Inventory item not found for product: " + itemRequest.getProductId()));

        // Check if item is active
        if (inventoryItem.getStatus() != InventoryItem.InventoryStatus.ACTIVE) {
            throw new IllegalArgumentException("Product is not available: " + itemRequest.getProductId());
        }

        // Check availability
        if (!inventoryItem.canReserve(itemRequest.getQuantity())) {
            throw new IllegalArgumentException(
                String.format("Insufficient stock for product %s. Requested: %d, Available: %d",
                    itemRequest.getProductId(), itemRequest.getQuantity(), inventoryItem.getAvailableQuantity()));
        }

        // Reserve stock in inventory item
        Integer previousAvailable = inventoryItem.getAvailableQuantity();
        Integer previousReserved = inventoryItem.getReservedQuantity();
        
        inventoryItem.reserveStock(itemRequest.getQuantity());
        inventoryItemRepository.save(inventoryItem);

        // Create reservation record
        InventoryReservation reservation = new InventoryReservation(
            tenantId, reservationId, orderId, inventoryItem.getId(),
            inventoryItem.getProductId(), inventoryItem.getSku(),
            itemRequest.getQuantity(), expiresAt);
        
        reservation = reservationRepository.save(reservation);

        // Log transaction
        stockTransactionService.logTransaction(
            tenantId, inventoryItem.getId(), StockTransaction.TransactionType.RESERVATION,
            itemRequest.getQuantity(), orderId, previousAvailable, inventoryItem.getAvailableQuantity(),
            previousReserved, inventoryItem.getReservedQuantity(), "ORDER", "Inventory reservation for order");

        return reservation;
    }

    /**
     * Confirm reservation (convert to actual stock deduction)
     */
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void confirmReservation(String tenantId, String reservationId) {
        List<InventoryReservation> reservations = reservationRepository
            .findActiveReservationsByOrder(tenantId, getOrderIdFromCache(tenantId, reservationId));

        if (reservations.isEmpty()) {
            throw new ResourceNotFoundException("No active reservations found for reservation ID: " + reservationId);
        }

        for (InventoryReservation reservation : reservations) {
            if (!reservation.canBeConfirmed()) {
                throw new IllegalStateException("Reservation cannot be confirmed: " + reservation.getReservationId());
            }

            // Confirm reservation in inventory item
            InventoryItem inventoryItem = inventoryItemRepository
                .findByTenantIdAndId(tenantId, reservation.getInventoryItemId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Inventory item not found: " + reservation.getInventoryItemId()));

            Integer previousReserved = inventoryItem.getReservedQuantity();
            inventoryItem.confirmReservation(reservation.getReservedQuantity());
            inventoryItemRepository.save(inventoryItem);

            // Update reservation status
            reservation.confirm();
            reservationRepository.save(reservation);

            // Log transaction
            stockTransactionService.logTransaction(
                tenantId, inventoryItem.getId(), StockTransaction.TransactionType.RESERVATION_CONFIRM,
                reservation.getReservedQuantity(), reservation.getOrderId(),
                inventoryItem.getAvailableQuantity(), inventoryItem.getAvailableQuantity(),
                previousReserved, inventoryItem.getReservedQuantity(),
                "ORDER", "Reservation confirmation for order");
        }

        // Remove from cache
        removeReservationFromCache(tenantId, reservationId);

        logger.info("Confirmed reservation: {}, items: {}", reservationId, reservations.size());
    }

    /**
     * Release reservation (return stock to available)
     */
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void releaseReservation(String tenantId, String reservationId, String reason) {
        List<InventoryReservation> reservations = reservationRepository
            .findActiveReservationsByOrder(tenantId, getOrderIdFromCache(tenantId, reservationId));

        if (reservations.isEmpty()) {
            throw new ResourceNotFoundException("No active reservations found for reservation ID: " + reservationId);
        }

        List<InventoryReleasedEvent.ReleasedItemData> releasedItems = new ArrayList<>();

        for (InventoryReservation reservation : reservations) {
            if (!reservation.canBeReleased()) {
                logger.warn("Reservation cannot be released: {}, status: {}", 
                           reservation.getReservationId(), reservation.getStatus());
                continue;
            }

            // Release reservation in inventory item
            InventoryItem inventoryItem = inventoryItemRepository
                .findByTenantIdAndId(tenantId, reservation.getInventoryItemId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Inventory item not found: " + reservation.getInventoryItemId()));

            Integer previousAvailable = inventoryItem.getAvailableQuantity();
            Integer previousReserved = inventoryItem.getReservedQuantity();
            
            inventoryItem.releaseReservation(reservation.getReservedQuantity());
            inventoryItemRepository.save(inventoryItem);

            // Update reservation status
            reservation.release();
            reservationRepository.save(reservation);

            // Log transaction
            stockTransactionService.logTransaction(
                tenantId, inventoryItem.getId(), StockTransaction.TransactionType.RESERVATION_RELEASE,
                reservation.getReservedQuantity(), reservation.getOrderId(),
                previousAvailable, inventoryItem.getAvailableQuantity(),
                previousReserved, inventoryItem.getReservedQuantity(),
                "ORDER", "Reservation release: " + reason);

            releasedItems.add(new InventoryReleasedEvent.ReleasedItemData(
                reservation.getProductId(),
                reservation.getSku(),
                reservation.getReservedQuantity(),
                reservation.getInventoryItemId()
            ));
        }

        // Remove from cache
        removeReservationFromCache(tenantId, reservationId);

        // Publish release event
        if (!releasedItems.isEmpty()) {
            InventoryReleasedEvent releaseEvent = new InventoryReleasedEvent(
                tenantId, reservations.get(0).getOrderId(), reservationId, releasedItems, reason);
            kafkaTemplate.send(inventoryEventsTopic, releaseEvent);
        }

        logger.info("Released reservation: {}, items: {}, reason: {}", 
                   reservationId, releasedItems.size(), reason);
    }

    /**
     * Get reservation by ID
     */
    @Transactional(readOnly = true)
    public ReservationResponse getReservation(String tenantId, String reservationId) {
        // Try cache first
        ReservationResponse cached = getReservationFromCache(tenantId, reservationId);
        if (cached != null) {
            return cached;
        }

        // Fallback to database
        List<InventoryReservation> reservations = reservationRepository
            .findActiveReservationsByOrder(tenantId, getOrderIdFromDatabase(tenantId, reservationId));

        if (reservations.isEmpty()) {
            throw new ResourceNotFoundException("Reservation not found: " + reservationId);
        }

        List<InventoryReservedEvent.ReservedItemData> items = reservations.stream()
            .map(r -> new InventoryReservedEvent.ReservedItemData(
                r.getProductId(), r.getSku(), r.getReservedQuantity(), r.getInventoryItemId()))
            .collect(Collectors.toList());

        return ReservationResponse.success(reservationId, items);
    }

    /**
     * Cleanup expired reservations
     */
    @Transactional
    public void cleanupExpiredReservations(String tenantId) {
        LocalDateTime now = LocalDateTime.now();
        List<InventoryReservation> expiredReservations = reservationRepository
            .findExpiredReservations(tenantId, now);

        for (InventoryReservation reservation : expiredReservations) {
            try {
                releaseReservation(tenantId, reservation.getReservationId(), "Expired");
            } catch (Exception e) {
                logger.error("Failed to cleanup expired reservation: {}", reservation.getReservationId(), e);
            }
        }

        logger.info("Cleaned up {} expired reservations for tenant: {}", expiredReservations.size(), tenantId);
    }

    // Private helper methods
    private void rollbackReservations(String tenantId, List<InventoryReservation> reservations) {
        for (InventoryReservation reservation : reservations) {
            try {
                InventoryItem inventoryItem = inventoryItemRepository
                    .findByTenantIdAndId(tenantId, reservation.getInventoryItemId())
                    .orElse(null);
                
                if (inventoryItem != null) {
                    inventoryItem.releaseReservation(reservation.getReservedQuantity());
                    inventoryItemRepository.save(inventoryItem);
                }
                
                reservationRepository.delete(reservation);
            } catch (Exception e) {
                logger.error("Failed to rollback reservation: {}", reservation.getId(), e);
            }
        }
    }

    private void cacheReservationData(String tenantId, String reservationId, List<InventoryReservation> reservations) {
        try {
            String cacheKey = RESERVATION_CACHE_PREFIX + tenantId + ":" + reservationId;
            List<InventoryReservedEvent.ReservedItemData> items = reservations.stream()
                .map(r -> new InventoryReservedEvent.ReservedItemData(
                    r.getProductId(), r.getSku(), r.getReservedQuantity(), r.getInventoryItemId()))
                .collect(Collectors.toList());
            
            ReservationResponse response = ReservationResponse.success(reservationId, items);
            redisTemplate.opsForValue().set(cacheKey, response, reservationTtlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.warn("Failed to cache reservation data: {}", reservationId, e);
        }
    }

    private ReservationResponse getReservationFromCache(String tenantId, String reservationId) {
        try {
            String cacheKey = RESERVATION_CACHE_PREFIX + tenantId + ":" + reservationId;
            return (ReservationResponse) redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            logger.warn("Failed to get reservation from cache: {}", reservationId, e);
            return null;
        }
    }

    private void removeReservationFromCache(String tenantId, String reservationId) {
        try {
            String cacheKey = RESERVATION_CACHE_PREFIX + tenantId + ":" + reservationId;
            redisTemplate.delete(cacheKey);
        } catch (Exception e) {
            logger.warn("Failed to remove reservation from cache: {}", reservationId, e);
        }
    }

    private String getOrderIdFromCache(String tenantId, String reservationId) {
        ReservationResponse cached = getReservationFromCache(tenantId, reservationId);
        return cached != null ? cached.getOrderId() : getOrderIdFromDatabase(tenantId, reservationId);
    }

    private String getOrderIdFromDatabase(String tenantId, String reservationId) {
        return reservationRepository.findByTenantIdAndReservationId(tenantId, reservationId)
            .map(InventoryReservation::getOrderId)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));
    }
}