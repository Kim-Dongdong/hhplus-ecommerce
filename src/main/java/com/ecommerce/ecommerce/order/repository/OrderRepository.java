package com.ecommerce.ecommerce.order.repository;

import com.ecommerce.ecommerce.order.domain.Order;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByUserId(Long userId);
}
