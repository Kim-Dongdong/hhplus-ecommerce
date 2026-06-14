package com.ecommerce.ecommerce.coupon.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.ecommerce.common.exception.BusinessException;
import com.ecommerce.ecommerce.common.exception.ErrorCode;
import com.ecommerce.ecommerce.coupon.dto.UserCouponResponse;
import com.ecommerce.ecommerce.coupon.service.CouponService;
import com.ecommerce.ecommerce.global.config.JpaAuditingConfig;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = CouponController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CouponService couponService;

    @Test
    @DisplayName("POST /api/v1/coupons/{couponId}/issue - 200 OK, 쿠폰 발급")
    void issue_정상발급_200OK() throws Exception {
        String requestBody = """
                {
                  "userId": 1
                }
                """;
        UserCouponResponse response = new UserCouponResponse(10L, 1L, 0.1, false, LocalDateTime.now(), LocalDateTime.now().plusDays(5));
        given(couponService.issueCoupon(eq(1L), eq(1L))).willReturn(response);

        mockMvc.perform(post("/api/v1/coupons/{couponId}/issue", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCouponId").value(10L))
                .andExpect(jsonPath("$.couponId").value(1L))
                .andExpect(jsonPath("$.discountRate").value(0.1))
                .andExpect(jsonPath("$.isUsed").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/coupons/{couponId}/issue - 선착순 초과 -> 409 Conflict")
    void issue_선착순초과_409Conflict() throws Exception {
        String requestBody = """
                {
                  "userId": 1
                }
                """;
        given(couponService.issueCoupon(eq(1L), eq(1L))).willThrow(new BusinessException(ErrorCode.COUPON_OUT_OF_STOCK));

        mockMvc.perform(post("/api/v1/coupons/{couponId}/issue", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COUPON_OUT_OF_STOCK"));
    }

    @Test
    @DisplayName("POST /api/v1/coupons/{couponId}/issue - 만료된 쿠폰 -> 400 Bad Request")
    void issue_만료된쿠폰_400BadRequest() throws Exception {
        String requestBody = """
                {
                  "userId": 1
                }
                """;
        given(couponService.issueCoupon(eq(1L), eq(1L))).willThrow(new BusinessException(ErrorCode.COUPON_EXPIRED));

        mockMvc.perform(post("/api/v1/coupons/{couponId}/issue", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COUPON_EXPIRED"));
    }

    @Test
    @DisplayName("POST /api/v1/coupons/{couponId}/issue - 중복 발급 -> 409 Conflict")
    void issue_중복발급_409Conflict() throws Exception {
        String requestBody = """
                {
                  "userId": 1
                }
                """;
        given(couponService.issueCoupon(eq(1L), eq(1L))).willThrow(new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED));

        mockMvc.perform(post("/api/v1/coupons/{couponId}/issue", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COUPON_ALREADY_ISSUED"));
    }

    @Test
    @DisplayName("POST /api/v1/coupons/{couponId}/issue - userId 누락 -> 400 Bad Request")
    void issue_userId누락_400BadRequest() throws Exception {
        String requestBody = """
                {
                }
                """;

        mockMvc.perform(post("/api/v1/coupons/{couponId}/issue", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
