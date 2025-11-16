package org.example.order.consumer;

import org.example.order.entity.Order;
import org.example.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PaymentEventConsumer {

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "payment-events", groupId = "order-service-group")
    public void handlePaymentEvent(@Payload Map<String, Object> paymentEvent, 
                                  @Header(KafkaHeaders.KEY) String eventType) {
        
        try {
            Long orderId = Long.valueOf(paymentEvent.get("orderId").toString());
            String eventTypeValue = paymentEvent.get("eventType").toString();
            
            switch (eventTypeValue) {
                case "PAYMENT_COMPLETED":
                    handlePaymentCompleted(orderId, paymentEvent);
                    break;
                case "PAYMENT_FAILED":
                    handlePaymentFailed(orderId, paymentEvent);
                    break;
                case "PAYMENT_REFUNDED":
                    handlePaymentRefunded(orderId, paymentEvent);
                    break;
                default:
                    System.out.println("Unknown payment event type: " + eventTypeValue);
            }
        } catch (Exception e) {
            System.err.println("Error processing payment event: " + e.getMessage());
        }
    }

    private void handlePaymentCompleted(Long orderId, Map<String, Object> event) {
        try {
            // Update order payment status to PAID
            Order order = orderService.updatePaymentStatus(orderId, Order.PaymentStatus.PAID);
            
            System.out.println("Payment completed for order: " + orderId);
            
            // Send notification event for successful payment
            kafkaTemplate.send("notification-events", "order-payment-success", 
                new NotificationEvent(order.getUserId(), "ORDER_PAYMENT_SUCCESS", 
                    "Your payment for order " + order.getOrderNumber() + " has been processed successfully.", 
                    orderId));
            
            // Clear the user's cart after successful payment
            kafkaTemplate.send("cart-events", "clear-cart-after-checkout", 
                Map.of("userId", order.getUserId(), "orderId", orderId));
                
        } catch (Exception e) {
            System.err.println("Error handling payment completed: " + e.getMessage());
        }
    }

    private void handlePaymentFailed(Long orderId, Map<String, Object> event) {
        try {
            // Update order payment status to FAILED
            Order order = orderService.updatePaymentStatus(orderId, Order.PaymentStatus.FAILED);
            
            String failureReason = event.get("failureReason") != null ? 
                event.get("failureReason").toString() : "Payment processing failed";
            
            System.out.println("Payment failed for order: " + orderId + ", reason: " + failureReason);
            
            // Send notification event for failed payment
            kafkaTemplate.send("notification-events", "order-payment-failed", 
                new NotificationEvent(order.getUserId(), "ORDER_PAYMENT_FAILED", 
                    "Payment for order " + order.getOrderNumber() + " failed: " + failureReason, 
                    orderId));
            
            // Restore product stock since order is cancelled
            for (var item : order.getItems()) {
                kafkaTemplate.send("inventory-events", "stock-restored", 
                    Map.of("productId", item.getProductId(), "quantity", item.getQuantity()));
            }
            
        } catch (Exception e) {
            System.err.println("Error handling payment failed: " + e.getMessage());
        }
    }

    private void handlePaymentRefunded(Long orderId, Map<String, Object> event) {
        try {
            // Update order payment status to REFUNDED
            Order order = orderService.updatePaymentStatus(orderId, Order.PaymentStatus.REFUNDED);
            
            System.out.println("Payment refunded for order: " + orderId);
            
            // Send notification event for refund
            kafkaTemplate.send("notification-events", "order-refunded", 
                new NotificationEvent(order.getUserId(), "ORDER_REFUNDED", 
                    "Your order " + order.getOrderNumber() + " has been refunded successfully.", 
                    orderId));
            
        } catch (Exception e) {
            System.err.println("Error handling payment refunded: " + e.getMessage());
        }
    }

    // Inner class for notification events
    public static class NotificationEvent {
        private Long userId;
        private String eventType;
        private String message;
        private Long orderId;
        
        public NotificationEvent(Long userId, String eventType, String message, Long orderId) {
            this.userId = userId;
            this.eventType = eventType;
            this.message = message;
            this.orderId = orderId;
        }
        
        // Getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
    }
}
