package com.ecommerce.orderservice.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SagaTimeoutHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(SagaTimeoutHandler.class);
    
    private final OrderSagaOrchestrator sagaOrchestrator;
    
    @Autowired
    public SagaTimeoutHandler(OrderSagaOrchestrator sagaOrchestrator) {
        this.sagaOrchestrator = sagaOrchestrator;
    }

    @Scheduled(fixedDelay = 60000) // Run every minute
    public void handleTimeouts() {
        try {
            logger.debug("Checking for timed out sagas");
            sagaOrchestrator.handleTimeouts();
        } catch (Exception e) {
            logger.error("Error handling saga timeouts: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = 300000) // Run every 5 minutes
    public void cleanupCompletedSagas() {
        try {
            logger.debug("Cleaning up completed sagas");
            sagaOrchestrator.cleanupCompletedSagas();
        } catch (Exception e) {
            logger.error("Error cleaning up completed sagas: {}", e.getMessage(), e);
        }
    }
}