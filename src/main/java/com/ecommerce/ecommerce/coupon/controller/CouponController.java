package com.ecommerce.ecommerce.coupon.controller;

import com.ecommerce.ecommerce.coupon.dto.CouponIssueRequest;
import com.ecommerce.ecommerce.coupon.dto.CouponRequest;
import com.ecommerce.ecommerce.coupon.dto.CouponResponse;
import com.ecommerce.ecommerce.coupon.dto.UserCouponResponse;
import com.ecommerce.ecommerce.coupon.service.CouponService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/coupons")
public class CouponController {

    private final CouponService couponService;

    @GetMapping
    public ResponseEntity<List<CouponResponse>> findAll() {
        return ResponseEntity.ok(couponService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CouponResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(couponService.findById(id));
    }

    @PostMapping
    public ResponseEntity<CouponResponse> create(@Valid @RequestBody CouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(couponService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CouponResponse> update(@PathVariable Long id, @Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(couponService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        couponService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{couponId}/issue")
    public ResponseEntity<UserCouponResponse> issue(@PathVariable Long couponId, @Valid @RequestBody CouponIssueRequest request) {
        return ResponseEntity.ok(couponService.issueCoupon(couponId, request.userId()));
    }
}
