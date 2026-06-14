package com.ecommerce.ecommerce.user.dto;

import com.ecommerce.ecommerce.user.domain.User;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
