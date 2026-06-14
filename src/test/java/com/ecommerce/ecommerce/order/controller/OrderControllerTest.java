package com.ecommerce.ecommerce.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.ecommerce.common.exception.BusinessException;
import com.ecommerce.ecommerce.common.exception.ErrorCode;
import com.ecommerce.ecommerce.global.config.JpaAuditingConfig;
import com.ecommerce.ecommerce.order.dto.OrderItemResponse;
import com.ecommerce.ecommerce.order.dto.OrderRequest;
import com.ecommerce.ecommerce.order.dto.OrderResponse;
import com.ecommerce.ecommerce.order.service.OrderService;
import java.time.LocalDateTime;
import java.util.List;
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
        controllers = OrderController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    @DisplayName("POST /api/v1/orders - 201 Created, 주문 생성 및 결제")
    void create_정상요청_201Created() throws Exception {
        String requestBody = """
                {
                  "userId": 1,
                  "items": [
                    { "productId": 1, "quantity": 2 },
                    { "productId": 2, "quantity": 1 }
                  ]
                }
                """;
        OrderResponse response = new OrderResponse(
                100L, 1L, 89_000, 89_000, "PAID", LocalDateTime.now(),
                List.of(new OrderItemResponse(1L, 2, 35_000), new OrderItemResponse(2L, 1, 19_000))
        );
        given(orderService.create(any(OrderRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(100L))
                .andExpect(jsonPath("$.totalAmount").value(89_000))
                .andExpect(jsonPath("$.discountedAmount").value(89_000))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    @DisplayName("POST /api/v1/orders - 잔액 부족 -> 402 Payment Required")
    void create_잔액부족_402PaymentRequired() throws Exception {
        String requestBody = """
                {
                  "userId": 1,
                  "items": [
                    { "productId": 1, "quantity": 2 }
                  ]
                }
                """;
        given(orderService.create(any(OrderRequest.class))).willThrow(new BusinessException(ErrorCode.INSUFFICIENT_BALANCE));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().is(402))
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"));
    }

    @Test
    @DisplayName("POST /api/v1/orders - 재고 부족 -> 409 Conflict")
    void create_재고부족_409Conflict() throws Exception {
        String requestBody = """
                {
                  "userId": 1,
                  "items": [
                    { "productId": 1, "quantity": 200 }
                  ]
                }
                """;
        given(orderService.create(any(OrderRequest.class))).willThrow(new BusinessException(ErrorCode.OUT_OF_STOCK));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OUT_OF_STOCK"));
    }

    @Test
    @DisplayName("POST /api/v1/orders - 필수 필드 누락 -> 400 Bad Request")
    void create_필수필드누락_400BadRequest() throws Exception {
        String requestBody = """
                {
                  "items": [
                    { "productId": 1, "quantity": 2 }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // TODO: GET /api/v1/orders, GET /api/v1/orders/{id}, PUT /api/v1/orders/{id}, DELETE /api/v1/orders/{id} 에 대한 컨트롤러 테스트 미작성
}
