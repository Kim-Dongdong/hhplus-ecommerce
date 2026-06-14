package com.ecommerce.ecommerce.coupon.dto;

import com.ecommerce.ecommerce.coupon.domain.UserCoupon;
import java.time.LocalDateTime;

public record UserCouponResponse(
        Long userCouponId,
        Long couponId,
        double discountRate,
        boolean isUsed,
        LocalDateTime issuedAt,
        LocalDateTime expiredAt
) {
    public static UserCouponResponse toResponse(UserCoupon userCoupon) {
        return new UserCouponResponse(
                userCoupon.getId(),
                userCoupon.getCouponId(),
                userCoupon.getDiscountRate(),
                userCoupon.isUsed(),
                userCoupon.getIssuedAt(),
                userCoupon.getExpiredAt()
        );
    }
}
