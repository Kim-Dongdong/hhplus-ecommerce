package com.ecommerce.ecommerce.product.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.ecommerce.global.config.JpaAuditingConfig;
import com.ecommerce.ecommerce.product.dto.ProductResponse;
import com.ecommerce.ecommerce.product.service.ProductService;
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
        controllers = ProductController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("GET /api/v1/products - 200 OK, 상품 목록 반환")
    void findAll_정상조회_200OK() throws Exception {
        ProductResponse response1 = new ProductResponse(1L, "무선 키보드", 35000, 120, LocalDateTime.now(), LocalDateTime.now());
        ProductResponse response2 = new ProductResponse(2L, "블루투스 마우스", 19000, 80, LocalDateTime.now(), LocalDateTime.now());
        given(productService.findAll()).willReturn(List.of(response1, response2));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("무선 키보드"))
                .andExpect(jsonPath("$[1].name").value("블루투스 마우스"));
    }

    // TODO: GET /api/v1/products/top - ProductController에 인기 상품 TOP N 조회 엔드포인트 미구현.
    // 구현 후 200 OK, 상위 5개 반환 테스트 추가 필요 (docs/api-spec.md 5번 참고)
}
