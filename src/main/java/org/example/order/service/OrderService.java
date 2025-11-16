package org.example.order.service;

import org.example.order.entity.Order;
import org.example.order.entity.OrderItem;
import org.example.order.repository.OrderRepository;
import org.example.order.client.CartServiceClient;
import org.example.order.dto.CartDto;
import org.example.order.dto.CartItemDto;
import org.example.order.dto.CheckoutRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private CartServiceClient cartServiceClient;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public Order createOrder(Long userId, CheckoutRequest checkoutRequest) {
        // Get cart from Cart Service
        CartDto cart = cartServiceClient.getCartForCheckout(userId);
        
        if (cart == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }
        
        // Generate order number
        String orderNumber = generateOrderNumber();
        
        // Create order
        Order order = new Order(orderNumber, userId, cart.getTotalAmount());
        order.setShippingAddress(checkoutRequest.getShippingAddress());
        order.setBillingAddress(checkoutRequest.getBillingAddress());
        
        // Calculate tax and shipping
        BigDecimal taxAmount = calculateTax(cart.getTotalAmount());
        BigDecimal shippingAmount = calculateShipping(cart.getTotalAmount());
        
        order.setTaxAmount(taxAmount);
        order.setShippingAmount(shippingAmount);
        order.setTotalAmount(cart.getTotalAmount().add(taxAmount).add(shippingAmount));
        
        // Add order items
        for (CartItemDto cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem(
                cartItem.getProductId(),
                cartItem.getProductName(),
                cartItem.getProductImage(),
                cartItem.getPrice(),
                cartItem.getQuantity()
            );
            order.addItem(orderItem);
        }
        
        // Save order
        Order savedOrder = orderRepository.save(order);
        
        // Send order created event to Kafka
        kafkaTemplate.send("order-events", "order-created", 
            new OrderEvent(savedOrder.getId(), userId, "ORDER_CREATED", savedOrder.getTotalAmount()));
        
        // Send payment request to Payment Service
        kafkaTemplate.send("payment-events", "payment-requested", 
            new PaymentRequest(savedOrder.getId(), userId, savedOrder.getTotalAmount(), 
                             checkoutRequest.getPaymentMethod(), checkoutRequest.getCardDetails()));
        
        return savedOrder;
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public Optional<Order> getOrderByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    public Page<Order> getOrdersByUserId(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public Order updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus(status);
            Order savedOrder = orderRepository.save(order);
            
            // Send order status updated event
            kafkaTemplate.send("order-events", "order-status-updated", 
                new OrderEvent(orderId, order.getUserId(), "ORDER_STATUS_UPDATED", order.getTotalAmount()));
            
            return savedOrder;
        }
        throw new RuntimeException("Order not found with id: " + orderId);
    }

    public Order updatePaymentStatus(Long orderId, Order.PaymentStatus paymentStatus) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setPaymentStatus(paymentStatus);
            
            if (paymentStatus == Order.PaymentStatus.PAID) {
                order.setStatus(Order.OrderStatus.CONFIRMED);
            } else if (paymentStatus == Order.PaymentStatus.FAILED) {
                order.setStatus(Order.OrderStatus.CANCELLED);
            }
            
            Order savedOrder = orderRepository.save(order);
            
            // Send payment status updated event
            kafkaTemplate.send("order-events", "payment-status-updated", 
                new OrderEvent(orderId, order.getUserId(), "PAYMENT_STATUS_UPDATED", order.getTotalAmount()));
            
            return savedOrder;
        }
        throw new RuntimeException("Order not found with id: " + orderId);
    }

    public Order updateTrackingNumber(Long orderId, String trackingNumber) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setTrackingNumber(trackingNumber);
            
            // If tracking number is provided, update status to shipped
            if (trackingNumber != null && !trackingNumber.trim().isEmpty()) {
                order.setStatus(Order.OrderStatus.SHIPPED);
            }
            
            Order savedOrder = orderRepository.save(order);
            
            // Send tracking updated event
            kafkaTemplate.send("order-events", "tracking-updated", 
                new OrderEvent(orderId, order.getUserId(), "TRACKING_UPDATED", order.getTotalAmount()));
            
            return savedOrder;
        }
        throw new RuntimeException("Order not found with id: " + orderId);
    }

    String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    BigDecimal calculateTax(BigDecimal amount) {
        // Simple tax calculation - 8.5%
        return amount.multiply(BigDecimal.valueOf(0.085));
    }

    BigDecimal calculateShipping(BigDecimal amount) {
        // Free shipping for orders over $50
        if (amount.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(9.99);
    }

    // Inner classes for Kafka events
    public static class OrderEvent {
        private Long orderId;
        private Long userId;
        private String eventType;
        private BigDecimal amount;
        private LocalDateTime timestamp;
        
        public OrderEvent(Long orderId, Long userId, String eventType, BigDecimal amount) {
            this.orderId = orderId;
            this.userId = userId;
            this.eventType = eventType;
            this.amount = amount;
            this.timestamp = LocalDateTime.now();
        }
        
        // Getters and setters
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    public static class PaymentRequest {
        private Long orderId;
        private Long userId;
        private BigDecimal amount;
        private String paymentMethod;
        private Object cardDetails;
        
        public PaymentRequest(Long orderId, Long userId, BigDecimal amount, 
                            String paymentMethod, Object cardDetails) {
            this.orderId = orderId;
            this.userId = userId;
            this.amount = amount;
            this.paymentMethod = paymentMethod;
            this.cardDetails = cardDetails;
        }
        
        // Getters and setters
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        
        public Object getCardDetails() { return cardDetails; }
        public void setCardDetails(Object cardDetails) { this.cardDetails = cardDetails; }
    }
}
