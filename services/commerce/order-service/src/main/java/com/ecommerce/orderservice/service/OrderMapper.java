package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.AddressDto;
import com.ecommerce.orderservice.dto.OrderItemResponse;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    private static final Logger logger = LoggerFactory.getLogger(OrderMapper.class);

    private final ObjectMapper objectMapper;

    @Autowired
    public OrderMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OrderResponse toResponse(Order order) {
        OrderResponse response = new OrderResponse();
        
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setUserId(order.getUserId());
        response.setStatus(order.getStatus());
        response.setSubtotal(order.getSubtotal());
        response.setTaxAmount(order.getTaxAmount());
        response.setShippingAmount(order.getShippingAmount());
        response.setTotalAmount(order.getTotalAmount());
        response.setCurrency(order.getCurrency());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        // Convert JSON addresses back to DTOs
        try {
            if (order.getBillingAddress() != null) {
                response.setBillingAddress(
                    objectMapper.readValue(order.getBillingAddress(), AddressDto.class));
            }
            if (order.getShippingAddress() != null) {
                response.setShippingAddress(
                    objectMapper.readValue(order.getShippingAddress(), AddressDto.class));
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse address JSON for order: {}", order.getId(), e);
            // Set empty addresses as fallback
            response.setBillingAddress(new AddressDto());
            response.setShippingAddress(new AddressDto());
        }

        // Map order items
        List<OrderItemResponse> itemResponses = order.getItems().stream()
            .map(this::toItemResponse)
            .collect(Collectors.toList());
        response.setItems(itemResponses);

        return response;
    }

    public OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
            item.getId(),
            item.getProductId(),
            item.getSku(),
            item.getProductName(),
            item.getQuantity(),
            item.getUnitPrice(),
            item.getTotalPrice()
        );
    }

    public List<OrderResponse> toResponseList(List<Order> orders) {
        return orders.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
}