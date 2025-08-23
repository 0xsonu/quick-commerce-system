package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.entity.InventoryReservation;
import com.ecommerce.inventoryservice.repository.InventoryReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationCleanupService.class);

    private final InventoryReservationRepository reservationRepository;
    private final InventoryReservationService reservationService;

    @Autowired
    public ReservationCleanupService(InventoryReservationRepository reservationRepository,
                                   InventoryReservationService reservationService) {
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
    }

    /**
     * Cleanup expired reservations every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void cleanupExpiredReservations() {
        logger.debug("Starting cleanup of expired reservations");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            List<InventoryReservation> expiredReservations = reservationRepository
                .findAllExpiredReservations(now);

            if (expiredReservations.isEmpty()) {
                logger.debug("No expired reservations found");
                return;
            }

            logger.info("Found {} expired reservations to cleanup", expiredReservations.size());

            int successCount = 0;
            int failureCount = 0;

            for (InventoryReservation reservation : expiredReservations) {
                try {
                    reservationService.releaseReservation(
                        reservation.getTenantId(), 
                        reservation.getReservationId(), 
                        "Expired - automatic cleanup"
                    );
                    successCount++;
                } catch (Exception e) {
                    logger.error("Failed to cleanup expired reservation: reservationId={}, orderId={}, error={}", 
                               reservation.getReservationId(), reservation.getOrderId(), e.getMessage());
                    failureCount++;
                }
            }

            logger.info("Completed cleanup of expired reservations: success={}, failures={}", 
                       successCount, failureCount);

        } catch (Exception e) {
            logger.error("Failed to cleanup expired reservations", e);
        }
    }

    /**
     * Update reservation status for expired reservations in bulk
     * This is a fallback cleanup in case individual releases fail
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    @Transactional
    public void bulkUpdateExpiredReservations() {
        logger.debug("Starting bulk update of expired reservations");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Update expired reservations to EXPIRED status
            int updatedCount = reservationRepository.updateExpiredReservations(
                null, // All tenants
                InventoryReservation.ReservationStatus.ACTIVE,
                InventoryReservation.ReservationStatus.EXPIRED,
                now
            );

            if (updatedCount > 0) {
                logger.info("Bulk updated {} expired reservations to EXPIRED status", updatedCount);
            } else {
                logger.debug("No expired reservations found for bulk update");
            }

        } catch (Exception e) {
            logger.error("Failed to bulk update expired reservations", e);
        }
    }

    /**
     * Log reservation statistics for monitoring
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes
    @Transactional(readOnly = true)
    public void logReservationStatistics() {
        try {
            // This would typically query across all tenants for monitoring
            // For now, we'll just log that the monitoring is active
            logger.info("Reservation cleanup service is active and monitoring expired reservations");
            
            // In a real implementation, you might want to:
            // 1. Query reservation statistics by tenant
            // 2. Send metrics to monitoring system
            // 3. Alert if too many reservations are expiring
            
        } catch (Exception e) {
            logger.error("Failed to log reservation statistics", e);
        }
    }
}