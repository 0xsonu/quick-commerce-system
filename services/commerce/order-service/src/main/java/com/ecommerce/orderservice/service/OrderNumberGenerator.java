package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.shared.utils.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderNumberGenerator {

    private static final Logger logger = LoggerFactory.getLogger(OrderNumberGenerator.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_RETRY_ATTEMPTS = 5;

    private final OrderRepository orderRepository;
    private final String orderPrefix;

    @Autowired
    public OrderNumberGenerator(OrderRepository orderRepository,
                               @Value("${app.order.number-prefix:ORD}") String orderPrefix) {
        this.orderRepository = orderRepository;
        this.orderPrefix = orderPrefix;
    }

    /**
     * Generates a unique order number with the following format:
     * {PREFIX}-{YYYYMMDD}-{TENANT_HASH}-{RANDOM}
     * 
     * Example: ORD-20240101-A1B2-C3D4E5F6
     */
    public String generateUniqueOrderNumber() {
        String tenantId = TenantContext.getTenantId();
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            String orderNumber = generateOrderNumber(tenantId);
            
            // Check if order number already exists
            if (!orderRepository.existsByOrderNumber(orderNumber)) {
                logger.debug("Generated unique order number: {} on attempt {}", orderNumber, attempt);
                return orderNumber;
            }
            
            logger.warn("Order number collision detected: {} (attempt {})", orderNumber, attempt);
        }
        
        // If we still have collisions after max attempts, add timestamp for uniqueness
        String fallbackOrderNumber = generateFallbackOrderNumber(tenantId);
        logger.warn("Using fallback order number generation: {}", fallbackOrderNumber);
        return fallbackOrderNumber;
    }

    private String generateOrderNumber(String tenantId) {
        String datePart = LocalDateTime.now().format(DATE_FORMAT);
        String tenantHash = generateTenantHash(tenantId);
        String randomPart = generateRandomPart();
        
        return String.format("%s-%s-%s-%s", orderPrefix, datePart, tenantHash, randomPart);
    }

    private String generateFallbackOrderNumber(String tenantId) {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String tenantHash = generateTenantHash(tenantId);
        String randomPart = generateRandomPart();
        String timestampPart = String.valueOf(System.nanoTime()).substring(8); // Last 10 digits
        
        return String.format("%s-%s-%s-%s-%s", orderPrefix, datePart, tenantHash, randomPart, timestampPart);
    }

    private String generateTenantHash(String tenantId) {
        // Generate a 4-character hash from tenant ID for readability
        int hash = Math.abs(tenantId.hashCode());
        String hexHash = Integer.toHexString(hash).toUpperCase();
        
        // Take first 4 characters, pad if necessary
        if (hexHash.length() >= 4) {
            return hexHash.substring(0, 4);
        } else {
            return String.format("%4s", hexHash).replace(' ', '0');
        }
    }

    private String generateRandomPart() {
        // Generate 8-character random alphanumeric string
        StringBuilder sb = new StringBuilder(8);
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        
        return sb.toString();
    }

    /**
     * Validates order number format
     */
    public boolean isValidOrderNumberFormat(String orderNumber) {
        if (orderNumber == null || orderNumber.trim().isEmpty()) {
            return false;
        }
        
        // Basic format validation: PREFIX-YYYYMMDD-XXXX-XXXXXXXX
        String[] parts = orderNumber.split("-");
        
        if (parts.length < 4) {
            return false;
        }
        
        // Check prefix
        if (!orderPrefix.equals(parts[0])) {
            return false;
        }
        
        // Check date part (8 digits)
        if (parts[1].length() != 8 || !parts[1].matches("\\d{8}")) {
            return false;
        }
        
        // Check tenant hash (4 alphanumeric characters)
        if (parts[2].length() != 4 || !parts[2].matches("[0-9A-F]{4}")) {
            return false;
        }
        
        // Check random part (8 alphanumeric characters)
        if (parts[3].length() != 8 || !parts[3].matches("[0-9A-Z]{8}")) {
            return false;
        }
        
        return true;
    }

    /**
     * Extracts date from order number for analytics
     */
    public LocalDateTime extractDateFromOrderNumber(String orderNumber) {
        if (!isValidOrderNumberFormat(orderNumber)) {
            throw new IllegalArgumentException("Invalid order number format: " + orderNumber);
        }
        
        String[] parts = orderNumber.split("-");
        String datePart = parts[1];
        
        try {
            return LocalDateTime.parse(datePart + "000000", DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date in order number: " + orderNumber, e);
        }
    }
}