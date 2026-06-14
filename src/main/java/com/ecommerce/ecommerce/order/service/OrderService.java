package com.ecommerce.ecommerce.order.service;

import com.ecommerce.ecommerce.common.exception.BusinessException;
import com.ecommerce.ecommerce.common.exception.ErrorCode;
import com.ecommerce.ecommerce.coupon.service.CouponService;
import com.ecommerce.ecommerce.order.domain.Order;
import com.ecommerce.ecommerce.order.domain.OrderItem;
import com.ecommerce.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.ecommerce.order.dto.OrderRequest;
import com.ecommerce.ecommerce.order.dto.OrderResponse;
import com.ecommerce.ecommerce.order.dto.OrderStatusRequest;
import com.ecommerce.ecommerce.order.repository.OrderRepository;
import com.ecommerce.ecommerce.product.domain.Product;
import com.ecommerce.ecommerce.product.repository.ProductRepository;
import com.ecommerce.ecommerce.user.domain.User;
import com.ecommerce.ecommerce.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CouponService couponService;

    // 재고 차감(상품별 비관적 락) -> 쿠폰 적용 -> 잔액 차감(유저 비관적 락) -> 주문 저장을 하나의 트랜잭션으로 처리
    @Transactional
    public OrderResponse create(OrderRequest request) {
        User user = userRepository.findByIdWithLock(request.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found. id=" + request.userId()));

        Order order = new Order(request.userId());
        int totalAmount = 0;
        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productRepository.findByIdWithLock(itemRequest.productId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Product not found. id=" + itemRequest.productId()));
            product.decreaseStock(itemRequest.quantity());

            int price = product.getPrice();
            totalAmount += price * itemRequest.quantity();
            order.addItem(new OrderItem(product.getId(), itemRequest.quantity(), price));
        }

        int discountedAmount = totalAmount;
        if (request.userCouponId() != null) {
            double discountRate = couponService.useCoupon(request.userCouponId(), request.userId());
            discountedAmount = (int) Math.round(totalAmount * (1 - discountRate));
        }

        user.decreaseBalance(discountedAmount);
        order.complete(totalAmount, discountedAmount);
        Order saved = orderRepository.save(order);

        // TODO: 결제 성공 후 주문 정보를 메시지 큐에 발행하여 데이터 플랫폼으로 비동기 전송 (응답을 막지 않아야 함)

        return OrderResponse.toResponse(saved);
    }

    public OrderResponse findById(Long id) {
        return OrderResponse.toResponse(getOrder(id));
    }

    public List<OrderResponse> findAll() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::toResponse)
                .toList();
    }

    // TODO: 주문 취소(CANCELLED) 시 재고/잔액/쿠폰 사용 상태 복구 로직 미구현
    @Transactional
    public OrderResponse update(Long id, OrderStatusRequest request) {
        Order order = getOrder(id);
        order.updateStatus(request.status());
        return OrderResponse.toResponse(order);
    }

    @Transactional
    public void delete(Long id) {
        orderRepository.delete(getOrder(id));
    }

    private Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Order not found. id=" + id));
    }
}
