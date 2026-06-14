package com.ecommerce.ecommerce.coupon.dto;

import jakarta.validation.constraints.NotNull;

public record CouponIssueRequest(
        @NotNull Long userId
) {
}
