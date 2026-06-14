package com.ecommerce.ecommerce.coupon.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CouponRequest(
        @NotBlank String name,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double discountRate,
        @NotNull @Min(1) Integer totalQuantity,
        @NotNull @Future LocalDateTime expiredAt
) {
}
