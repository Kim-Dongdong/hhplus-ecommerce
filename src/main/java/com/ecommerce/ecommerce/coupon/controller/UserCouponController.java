package com.ecommerce.ecommerce.coupon.controller;

import com.ecommerce.ecommerce.coupon.dto.UserCouponResponse;
import com.ecommerce.ecommerce.coupon.service.CouponService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserCouponController {

    private final CouponService couponService;

    @GetMapping("/{userId}/coupons")
    public ResponseEntity<List<UserCouponResponse>> getUserCoupons(@PathVariable Long userId) {
        return ResponseEntity.ok(couponService.getUserCoupons(userId));
    }
}
