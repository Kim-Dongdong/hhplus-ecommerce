package com.ecommerce.ecommerce.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ecommerce.ecommerce.common.exception.BusinessException;
import com.ecommerce.ecommerce.common.exception.ErrorCode;
import com.ecommerce.ecommerce.coupon.domain.Coupon;
import com.ecommerce.ecommerce.coupon.domain.UserCoupon;
import com.ecommerce.ecommerce.coupon.dto.UserCouponResponse;
import com.ecommerce.ecommerce.coupon.repository.CouponRepository;
import com.ecommerce.ecommerce.coupon.repository.UserCouponRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @InjectMocks
    private CouponService couponService;

    @Test
    @DisplayName("issueCoupon_정상발급_userCoupon생성")
    void issueCoupon_정상발급_userCoupon생성() {
        Coupon coupon = new Coupon("10% 할인 쿠폰", 0.1, 1, LocalDateTime.now().plusDays(1));
        given(couponRepository.findByIdWithLock(1L)).willReturn(Optional.of(coupon));
        given(userCouponRepository.existsByUserIdAndCouponId(1L, 1L)).willReturn(false);
        given(userCouponRepository.save(any(UserCoupon.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        UserCouponResponse result = couponService.issueCoupon(1L, 1L);

        assertThat(result.couponId()).isEqualTo(coupon.getId());
        assertThat(result.discountRate()).isEqualTo(0.1);
        assertThat(coupon.getIssuedQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("issueCoupon_선착순초과_예외발생")
    void issueCoupon_선착순초과_예외발생() {
        Coupon coupon = new Coupon("품절 쿠폰", 0.1, 0, LocalDateTime.now().plusDays(1));
        given(couponRepository.findByIdWithLock(1L)).willReturn(Optional.of(coupon));
        given(userCouponRepository.existsByUserIdAndCouponId(1L, 1L)).willReturn(false);

        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.COUPON_OUT_OF_STOCK));
        verify(userCouponRepository, never()).save(any(UserCoupon.class));
    }

    @Test
    @DisplayName("issueCoupon_만료된쿠폰_예외발생")
    void issueCoupon_만료된쿠폰_예외발생() {
        Coupon coupon = new Coupon("만료된 쿠폰", 0.1, 10, LocalDateTime.now().minusDays(1));
        given(couponRepository.findByIdWithLock(1L)).willReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.COUPON_EXPIRED));
        verify(userCouponRepository, never()).save(any(UserCoupon.class));
    }

    @Test
    @DisplayName("issueCoupon_중복발급_예외발생")
    void issueCoupon_중복발급_예외발생() {
        Coupon coupon = new Coupon("10% 할인 쿠폰", 0.1, 10, LocalDateTime.now().plusDays(1));
        given(couponRepository.findByIdWithLock(1L)).willReturn(Optional.of(coupon));
        given(userCouponRepository.existsByUserIdAndCouponId(1L, 1L)).willReturn(true);

        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.COUPON_ALREADY_ISSUED));
        verify(userCouponRepository, never()).save(any(UserCoupon.class));
    }

    @Test
    @DisplayName("issueCoupon_존재하지않는쿠폰_예외발생")
    void issueCoupon_존재하지않는쿠폰_예외발생() {
        given(couponRepository.findByIdWithLock(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    @DisplayName("getUserCoupons_정상조회_보유쿠폰반환")
    void getUserCoupons_정상조회_보유쿠폰반환() {
        UserCoupon userCoupon1 = new UserCoupon(1L, 1L, 0.1, LocalDateTime.now().plusDays(5));
        UserCoupon userCoupon2 = new UserCoupon(1L, 2L, 0.2, LocalDateTime.now().plusDays(10));
        given(userCouponRepository.findAllByUserId(1L)).willReturn(List.of(userCoupon1, userCoupon2));

        List<UserCouponResponse> result = couponService.getUserCoupons(1L);

        assertThat(result).hasSize(2)
                .extracting(UserCouponResponse::couponId)
                .containsExactly(1L, 2L);
    }
}
