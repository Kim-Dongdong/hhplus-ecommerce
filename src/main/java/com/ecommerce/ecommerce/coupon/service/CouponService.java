package com.ecommerce.ecommerce.coupon.service;

import com.ecommerce.ecommerce.common.exception.BusinessException;
import com.ecommerce.ecommerce.common.exception.ErrorCode;
import com.ecommerce.ecommerce.coupon.domain.Coupon;
import com.ecommerce.ecommerce.coupon.domain.UserCoupon;
import com.ecommerce.ecommerce.coupon.dto.CouponRequest;
import com.ecommerce.ecommerce.coupon.dto.CouponResponse;
import com.ecommerce.ecommerce.coupon.dto.UserCouponResponse;
import com.ecommerce.ecommerce.coupon.repository.CouponRepository;
import com.ecommerce.ecommerce.coupon.repository.UserCouponRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    @Transactional
    public CouponResponse create(CouponRequest request) {
        Coupon coupon = new Coupon(request.name(), request.discountRate(), request.totalQuantity(), request.expiredAt());
        Coupon saved = couponRepository.save(coupon);
        return CouponResponse.toResponse(saved);
    }

    public CouponResponse findById(Long id) {
        return CouponResponse.toResponse(getCoupon(id));
    }

    public List<CouponResponse> findAll() {
        return couponRepository.findAll().stream()
                .map(CouponResponse::toResponse)
                .toList();
    }

    @Transactional
    public CouponResponse update(Long id, CouponRequest request) {
        Coupon coupon = getCoupon(id);
        coupon.update(request.name(), request.discountRate(), request.totalQuantity(), request.expiredAt());
        return CouponResponse.toResponse(coupon);
    }

    @Transactional
    public void delete(Long id) {
        couponRepository.delete(getCoupon(id));
    }

    // 선착순 발급: 비관적 락으로 동시 발급 시 issuedQuantity가 totalQuantity를 초과하지 않도록 보장
    @Transactional
    public UserCouponResponse issueCoupon(Long couponId, Long userId) {
        Coupon coupon = couponRepository.findByIdWithLock(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Coupon not found. id=" + couponId));

        if (coupon.isExpired()) {
            throw new BusinessException(ErrorCode.COUPON_EXPIRED);
        }
        if (userCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
        }
        if (coupon.isSoldOut()) {
            throw new BusinessException(ErrorCode.COUPON_OUT_OF_STOCK);
        }

        coupon.increaseIssuedQuantity();

        UserCoupon userCoupon = new UserCoupon(userId, coupon.getId(), coupon.getDiscountRate(), coupon.getExpiredAt());
        UserCoupon saved = userCouponRepository.save(userCoupon);
        return UserCouponResponse.toResponse(saved);
    }

    // 주문에서 쿠폰 적용 시 사용: 소유/미사용/유효기간을 검증하고 사용 처리한 뒤 할인율을 반환
    @Transactional
    public double useCoupon(Long userCouponId, Long userId) {
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_COUPON, "UserCoupon not found. id=" + userCouponId));

        if (!userCoupon.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_COUPON, "Coupon does not belong to user. userCouponId=" + userCouponId);
        }
        if (userCoupon.isUsed()) {
            throw new BusinessException(ErrorCode.INVALID_COUPON, "Coupon already used. userCouponId=" + userCouponId);
        }
        if (LocalDateTime.now().isAfter(userCoupon.getExpiredAt())) {
            throw new BusinessException(ErrorCode.INVALID_COUPON, "Coupon expired. userCouponId=" + userCouponId);
        }

        userCoupon.use();
        return userCoupon.getDiscountRate();
    }

    public List<UserCouponResponse> getUserCoupons(Long userId) {
        return userCouponRepository.findAllByUserId(userId).stream()
                .map(UserCouponResponse::toResponse)
                .toList();
    }

    private Coupon getCoupon(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Coupon not found. id=" + id));
    }
}
