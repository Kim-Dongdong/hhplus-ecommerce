package com.ecommerce.ecommerce.order.dto;

import com.ecommerce.ecommerce.order.domain.OrderItem;

public record OrderItemResponse(
        Long productId,
        int quantity,
        int price
) {
    public static OrderItemResponse toResponse(OrderItem item) {
        return new OrderItemResponse(item.getProductId(), item.getQuantity(), item.getPrice());
    }
}
