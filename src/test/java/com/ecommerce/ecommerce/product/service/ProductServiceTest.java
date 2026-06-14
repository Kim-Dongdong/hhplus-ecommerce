package com.ecommerce.ecommerce.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.ecommerce.ecommerce.common.exception.BusinessException;
import com.ecommerce.ecommerce.common.exception.ErrorCode;
import com.ecommerce.ecommerce.product.domain.Product;
import com.ecommerce.ecommerce.product.dto.ProductResponse;
import com.ecommerce.ecommerce.product.repository.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("findAll_정상조회_상품목록반환")
    void findAll_정상조회_상품목록반환() {
        Product product1 = new Product("무선 키보드", 35000, 120);
        Product product2 = new Product("블루투스 마우스", 19000, 80);
        given(productRepository.findAll()).willReturn(List.of(product1, product2));

        List<ProductResponse> result = productService.findAll();

        assertThat(result).hasSize(2)
                .extracting(ProductResponse::name)
                .containsExactly("무선 키보드", "블루투스 마우스");
    }

    @Test
    @DisplayName("decreaseStock_정상케이스_재고차감")
    void decreaseStock_정상케이스_재고차감() {
        Product product = new Product("무선 키보드", 35000, 120);
        given(productRepository.findByIdWithLock(1L)).willReturn(Optional.of(product));

        productService.decreaseStock(1L, 20);

        assertThat(product.getStock()).isEqualTo(100);
    }

    @Test
    @DisplayName("decreaseStock_재고부족_예외발생")
    void decreaseStock_재고부족_예외발생() {
        Product product = new Product("무선 키보드", 35000, 10);
        given(productRepository.findByIdWithLock(1L)).willReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.decreaseStock(1L, 20))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.OUT_OF_STOCK));
    }

    @Test
    @DisplayName("decreaseStock_존재하지않는상품_예외발생")
    void decreaseStock_존재하지않는상품_예외발생() {
        given(productRepository.findByIdWithLock(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.decreaseStock(1L, 1))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    // TODO: findTopProducts_정상조회_상위5개반환 - ProductService에 findTopProducts (최근 N일 인기 상품 TOP N) 미구현.
    // /api/v1/products/top 엔드포인트 및 집계 로직 구현 후 테스트 추가 필요 (docs/api-spec.md 5번 참고)
}
