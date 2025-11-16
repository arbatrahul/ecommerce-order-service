package org.example.order.repository;

import org.example.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);
    
    Page<Order> findByPaymentStatus(Order.PaymentStatus paymentStatus, Pageable pageable);
}
