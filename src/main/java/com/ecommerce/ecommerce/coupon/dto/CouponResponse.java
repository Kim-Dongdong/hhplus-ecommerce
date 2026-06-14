package com.ecommerce.ecommerce.coupon.dto;

import com.ecommerce.ecommerce.coupon.domain.Coupon;
import java.time.LocalDateTime;

public record CouponResponse(
        Long id,
        String name,
        double discountRate,
        int totalQuantity,
        int issuedQuantity,
        LocalDateTime expiredAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CouponResponse toResponse(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getName(),
                coupon.getDiscountRate(),
                coupon.getTotalQuantity(),
                coupon.getIssuedQuantity(),
                coupon.getExpiredAt(),
                coupon.getCreatedAt(),
                coupon.getUpdatedAt()
        );
    }
}
