package com.ecommerce.ecommerce.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ecommerce.ecommerce.common.exception.BusinessException;
import com.ecommerce.ecommerce.common.exception.ErrorCode;
import com.ecommerce.ecommerce.coupon.service.CouponService;
import com.ecommerce.ecommerce.order.domain.Order;
import com.ecommerce.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.ecommerce.order.dto.OrderRequest;
import com.ecommerce.ecommerce.order.dto.OrderResponse;
import com.ecommerce.ecommerce.order.repository.OrderRepository;
import com.ecommerce.ecommerce.product.domain.Product;
import com.ecommerce.ecommerce.product.repository.ProductRepository;
import com.ecommerce.ecommerce.user.domain.User;
import com.ecommerce.ecommerce.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CouponService couponService;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("create_정상케이스_주문생성및결제")
    void create_정상케이스_주문생성및결제() {
        User user = new User("user@test.com", "password", "테스터");
        user.chargeBalance(100_000);
        Product product = new Product("무선 키보드", 35_000, 120);
        OrderRequest request = new OrderRequest(1L, List.of(new OrderItemRequest(1L, 2)), null);

        given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user));
        given(productRepository.findByIdWithLock(1L)).willReturn(Optional.of(product));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        OrderResponse result = orderService.create(request);

        assertThat(result.totalAmount()).isEqualTo(70_000);
        assertThat(result.discountedAmount()).isEqualTo(70_000);
        assertThat(result.status()).isEqualTo("PAID");
        assertThat(product.getStock()).isEqualTo(118);
        assertThat(user.getBalance()).isEqualTo(30_000);
    }

    @Test
    @DisplayName("create_잔액부족_예외발생")
    void create_잔액부족_예외발생() {
        User user = new User("user@test.com", "password", "테스터");
        user.chargeBalance(10_000);
        Product product = new Product("무선 키보드", 35_000, 120);
        OrderRequest request = new OrderRequest(1L, List.of(new OrderItemRequest(1L, 1)), null);

        given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user));
        given(productRepository.findByIdWithLock(1L)).willReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("create_재고부족_예외발생")
    void create_재고부족_예외발생() {
        User user = new User("user@test.com", "password", "테스터");
        user.chargeBalance(100_000);
        Product product = new Product("무선 키보드", 35_000, 1);
        OrderRequest request = new OrderRequest(1L, List.of(new OrderItemRequest(1L, 2)), null);

        given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user));
        given(productRepository.findByIdWithLock(1L)).willReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.OUT_OF_STOCK));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("create_유효하지않은쿠폰_예외발생")
    void create_유효하지않은쿠폰_예외발생() {
        User user = new User("user@test.com", "password", "테스터");
        user.chargeBalance(100_000);
        Product product = new Product("무선 키보드", 35_000, 120);
        OrderRequest request = new OrderRequest(1L, List.of(new OrderItemRequest(1L, 1)), 10L);

        given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user));
        given(productRepository.findByIdWithLock(1L)).willReturn(Optional.of(product));
        given(couponService.useCoupon(10L, 1L)).willThrow(new BusinessException(ErrorCode.INVALID_COUPON));

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_COUPON));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("create_쿠폰적용_할인금액계산")
    void create_쿠폰적용_할인금액계산() {
        User user = new User("user@test.com", "password", "테스터");
        user.chargeBalance(100_000);
        Product product = new Product("무선 키보드", 35_000, 120);
        OrderRequest request = new OrderRequest(1L, List.of(new OrderItemRequest(1L, 2)), 10L);

        given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user));
        given(productRepository.findByIdWithLock(1L)).willReturn(Optional.of(product));
        given(couponService.useCoupon(10L, 1L)).willReturn(0.1);
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        OrderResponse result = orderService.create(request);

        assertThat(result.totalAmount()).isEqualTo(70_000);
        assertThat(result.discountedAmount()).isEqualTo(63_000);
        assertThat(user.getBalance()).isEqualTo(37_000);
    }

    // TODO: 주문 취소(update -> CANCELLED) 시 재고/잔액/쿠폰 복구 로직 미구현 (OrderService.update 참고)
}
