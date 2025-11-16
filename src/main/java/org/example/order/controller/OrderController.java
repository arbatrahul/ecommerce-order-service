package org.example.order.controller;

import org.example.order.entity.Order;
import org.example.order.dto.CheckoutRequest;
import org.example.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // Checkout - Create order from cart
    @PostMapping("/checkout/{userId}")
    public ResponseEntity<Map<String, Object>> checkout(
            @PathVariable Long userId,
            @Valid @RequestBody CheckoutRequest checkoutRequest) {
        
        try {
            Order order = orderService.createOrder(userId, checkoutRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order created successfully");
            response.put("order", order);
            response.put("orderNumber", order.getOrderNumber());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Get order by ID
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        Optional<Order> order = orderService.getOrderById(id);
        return order.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    // Get order by order number
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<Order> getOrderByOrderNumber(@PathVariable String orderNumber) {
        Optional<Order> order = orderService.getOrderByOrderNumber(orderNumber);
        return order.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    // Get user's order history
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserOrders(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orderPage = orderService.getOrdersByUserId(userId, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("orders", orderPage.getContent());
        response.put("currentPage", orderPage.getNumber());
        response.put("totalItems", orderPage.getTotalElements());
        response.put("totalPages", orderPage.getTotalPages());
        response.put("hasNext", orderPage.hasNext());
        response.put("hasPrevious", orderPage.hasPrevious());
        
        return ResponseEntity.ok(response);
    }

    // Update order status (Admin)
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam Order.OrderStatus status) {
        
        try {
            Order order = orderService.updateOrderStatus(id, status);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order status updated successfully");
            response.put("order", order);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Update payment status (Internal - called by Payment Service)
    @PutMapping("/{id}/payment-status")
    public ResponseEntity<Map<String, Object>> updatePaymentStatus(
            @PathVariable Long id,
            @RequestParam Order.PaymentStatus paymentStatus) {
        
        try {
            Order order = orderService.updatePaymentStatus(id, paymentStatus);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment status updated successfully");
            response.put("order", order);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Track order by order number (Public endpoint)
    @GetMapping("/track/{orderNumber}")
    public ResponseEntity<Map<String, Object>> trackOrder(@PathVariable String orderNumber) {
        try {
            Optional<Order> orderOpt = orderService.getOrderByOrderNumber(orderNumber);
            if (orderOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Order not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            Order order = orderOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderNumber", order.getOrderNumber());
            response.put("status", order.getStatus());
            response.put("paymentStatus", order.getPaymentStatus());
            response.put("trackingNumber", order.getTrackingNumber());
            response.put("shippingAddress", order.getShippingAddress());
            response.put("estimatedDelivery", order.getEstimatedDeliveryDate());
            response.put("createdAt", order.getCreatedAt());
            response.put("updatedAt", order.getUpdatedAt());
            
            // Add tracking timeline
            response.put("trackingTimeline", createTrackingTimeline(order));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error tracking order: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Track order by order ID (for authenticated users)
    @GetMapping("/{orderId}/track")
    public ResponseEntity<Map<String, Object>> trackOrderById(@PathVariable Long orderId) {
        try {
            Optional<Order> orderOpt = orderService.getOrderById(orderId);
            if (orderOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Order not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            Order order = orderOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("order", order);
            response.put("trackingTimeline", createTrackingTimeline(order));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error tracking order: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Update tracking number (Admin endpoint)
    @PutMapping("/{orderId}/tracking")
    public ResponseEntity<Map<String, Object>> updateTrackingNumber(
            @PathVariable Long orderId,
            @RequestParam String trackingNumber) {
        
        try {
            Order updatedOrder = orderService.updateTrackingNumber(orderId, trackingNumber);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tracking number updated successfully");
            response.put("order", updatedOrder);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Helper method to create tracking timeline
    private java.util.List<Map<String, Object>> createTrackingTimeline(Order order) {
        java.util.List<Map<String, Object>> timeline = new java.util.ArrayList<>();
        
        // Order placed
        Map<String, Object> orderPlaced = new HashMap<>();
        orderPlaced.put("status", "Order Placed");
        orderPlaced.put("description", "Your order has been placed successfully");
        orderPlaced.put("timestamp", order.getCreatedAt());
        orderPlaced.put("completed", true);
        timeline.add(orderPlaced);
        
        // Order confirmed
        Map<String, Object> orderConfirmed = new HashMap<>();
        orderConfirmed.put("status", "Order Confirmed");
        orderConfirmed.put("description", "Your order has been confirmed and is being prepared");
        orderConfirmed.put("timestamp", order.getUpdatedAt());
        orderConfirmed.put("completed", order.getStatus() != Order.OrderStatus.PENDING);
        timeline.add(orderConfirmed);
        
        // Processing
        Map<String, Object> processing = new HashMap<>();
        processing.put("status", "Processing");
        processing.put("description", "Your order is being processed");
        processing.put("timestamp", order.getUpdatedAt());
        processing.put("completed", order.getStatus() == Order.OrderStatus.PROCESSING || 
                                   order.getStatus() == Order.OrderStatus.SHIPPED || 
                                   order.getStatus() == Order.OrderStatus.DELIVERED);
        timeline.add(processing);
        
        // Shipped
        Map<String, Object> shipped = new HashMap<>();
        shipped.put("status", "Shipped");
        shipped.put("description", "Your order has been shipped");
        if (order.getTrackingNumber() != null) {
            shipped.put("description", "Your order has been shipped. Tracking number: " + order.getTrackingNumber());
        }
        shipped.put("timestamp", order.getUpdatedAt());
        shipped.put("completed", order.getStatus() == Order.OrderStatus.SHIPPED || 
                                 order.getStatus() == Order.OrderStatus.DELIVERED);
        timeline.add(shipped);
        
        // Delivered
        Map<String, Object> delivered = new HashMap<>();
        delivered.put("status", "Delivered");
        delivered.put("description", "Your order has been delivered");
        delivered.put("timestamp", order.getUpdatedAt());
        delivered.put("completed", order.getStatus() == Order.OrderStatus.DELIVERED);
        timeline.add(delivered);
        
        return timeline;
    }
}
