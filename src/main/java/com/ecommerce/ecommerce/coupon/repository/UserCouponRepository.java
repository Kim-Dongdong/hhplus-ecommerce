package com.ecommerce.ecommerce.coupon.repository;

import com.ecommerce.ecommerce.coupon.domain.UserCoupon;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    List<UserCoupon> findAllByUserId(Long userId);

    boolean existsByUserIdAndCouponId(Long userId, Long couponId);
}
