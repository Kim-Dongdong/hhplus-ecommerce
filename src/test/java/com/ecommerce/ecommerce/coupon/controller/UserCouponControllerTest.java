package com.ecommerce.ecommerce.coupon.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.ecommerce.coupon.dto.UserCouponResponse;
import com.ecommerce.ecommerce.coupon.service.CouponService;
import com.ecommerce.ecommerce.global.config.JpaAuditingConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = UserCouponController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
class UserCouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CouponService couponService;

    @Test
    @DisplayName("GET /api/v1/users/{userId}/coupons - 200 OK, 보유 쿠폰 목록 반환")
    void getUserCoupons_정상조회_200OK() throws Exception {
        UserCouponResponse response = new UserCouponResponse(10L, 1L, 0.1, false, LocalDateTime.now(), LocalDateTime.now().plusDays(5));
        given(couponService.getUserCoupons(1L)).willReturn(List.of(response));

        mockMvc.perform(get("/api/v1/users/{userId}/coupons", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userCouponId").value(10L))
                .andExpect(jsonPath("$[0].couponId").value(1L))
                .andExpect(jsonPath("$[0].isUsed").value(false));
    }
}
