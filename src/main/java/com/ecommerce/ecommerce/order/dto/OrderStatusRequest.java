package com.ecommerce.ecommerce.order.dto;

import com.ecommerce.ecommerce.order.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusRequest(
        @NotNull OrderStatus status
) {
}
