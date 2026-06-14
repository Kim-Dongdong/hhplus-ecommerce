package com.ecommerce.ecommerce.order.dto;

import com.ecommerce.ecommerce.order.domain.Order;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long orderId,
        Long userId,
        int totalAmount,
        int discountedAmount,
        String status,
        LocalDateTime orderedAt,
        List<OrderItemResponse> items
) {
    public static OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getDiscountedAmount(),
                order.getStatus().name(),
                order.getOrderedAt(),
                order.getItems().stream().map(OrderItemResponse::toResponse).toList()
        );
    }
}
