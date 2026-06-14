package com.ecommerce.ecommerce.product.dto;

import com.ecommerce.ecommerce.product.domain.Product;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        int price,
        int stock,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
