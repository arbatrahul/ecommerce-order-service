package org.example.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.order.dto.CheckoutRequest;
import org.example.order.entity.Order;
import org.example.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    private Order order;
    private CheckoutRequest checkoutRequest;

    @BeforeEach
    void setUp() {
        // Setup Order
        order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD-123456789-ABCD1234");
        order.setUserId(1L);
        order.setTotalAmount(new BigDecimal("99.99"));
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentStatus(Order.PaymentStatus.PENDING);
        order.setShippingAddress("123 Test St, Test City, TC 12345");
        order.setBillingAddress("123 Test St, Test City, TC 12345");
        order.setTrackingNumber("TRACK123456");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // Setup Checkout Request
        checkoutRequest = new CheckoutRequest();
        checkoutRequest.setShippingAddress("123 Test St, Test City, TC 12345");
        checkoutRequest.setBillingAddress("123 Test St, Test City, TC 12345");
        checkoutRequest.setPaymentMethod("CREDIT_CARD");
        
        CheckoutRequest.CardDetails cardDetails = new CheckoutRequest.CardDetails();
        cardDetails.setCardNumber("4111111111111111");
        cardDetails.setCardHolderName("Test User");
        cardDetails.setExpiryMonth("12");
        cardDetails.setExpiryYear("2025");
        cardDetails.setCvv("123");
        checkoutRequest.setCardDetails(cardDetails);
    }

    @Test
    void checkout_Success() throws Exception {
        // Given
        when(orderService.createOrder(eq(1L), any(CheckoutRequest.class))).thenReturn(order);

        // When & Then
        mockMvc.perform(post("/api/orders/checkout/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(checkoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Order created successfully"))
                .andExpect(jsonPath("$.orderNumber").value("ORD-123456789-ABCD1234"));

        verify(orderService).createOrder(eq(1L), any(CheckoutRequest.class));
    }

    @Test
    void checkout_ValidationError() throws Exception {
        // Given
        checkoutRequest.setShippingAddress(""); // Invalid address

        // When & Then
        mockMvc.perform(post("/api/orders/checkout/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(checkoutRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkout_ServiceError() throws Exception {
        // Given
        when(orderService.createOrder(eq(1L), any(CheckoutRequest.class)))
                .thenThrow(new RuntimeException("Cart is empty"));

        // When & Then
        mockMvc.perform(post("/api/orders/checkout/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(checkoutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cart is empty"));
    }

    @Test
    void getOrderById_Success() throws Exception {
        // Given
        when(orderService.getOrderById(1L)).thenReturn(Optional.of(order));

        // When & Then
        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderNumber").value("ORD-123456789-ABCD1234"))
                .andExpect(jsonPath("$.userId").value(1));

        verify(orderService).getOrderById(1L);
    }

    @Test
    void getOrderById_NotFound() throws Exception {
        // Given
        when(orderService.getOrderById(1L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isNotFound());

        verify(orderService).getOrderById(1L);
    }

    @Test
    void getOrderByOrderNumber_Success() throws Exception {
        // Given
        when(orderService.getOrderByOrderNumber("ORD-123456789-ABCD1234")).thenReturn(Optional.of(order));

        // When & Then
        mockMvc.perform(get("/api/orders/number/ORD-123456789-ABCD1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderNumber").value("ORD-123456789-ABCD1234"));

        verify(orderService).getOrderByOrderNumber("ORD-123456789-ABCD1234");
    }

    @Test
    void getUserOrders_Success() throws Exception {
        // Given
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(order));
        when(orderService.getOrdersByUserId(eq(1L), any(PageRequest.class))).thenReturn(orderPage);

        // When & Then
        mockMvc.perform(get("/api/orders/user/1")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders[0].id").value(1))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(orderService).getOrdersByUserId(eq(1L), any(PageRequest.class));
    }

    @Test
    void updateOrderStatus_Success() throws Exception {
        // Given
        order.setStatus(Order.OrderStatus.CONFIRMED);
        when(orderService.updateOrderStatus(1L, Order.OrderStatus.CONFIRMED)).thenReturn(order);

        // When & Then
        mockMvc.perform(put("/api/orders/1/status")
                .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Order status updated successfully"));

        verify(orderService).updateOrderStatus(1L, Order.OrderStatus.CONFIRMED);
    }

    @Test
    void updateOrderStatus_OrderNotFound() throws Exception {
        // Given
        when(orderService.updateOrderStatus(1L, Order.OrderStatus.CONFIRMED))
                .thenThrow(new RuntimeException("Order not found with id: 1"));

        // When & Then
        mockMvc.perform(put("/api/orders/1/status")
                .param("status", "CONFIRMED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Order not found with id: 1"));
    }

    @Test
    void trackOrder_Success() throws Exception {
        // Given
        order.setStatus(Order.OrderStatus.SHIPPED);
        when(orderService.getOrderByOrderNumber("ORD-123456789-ABCD1234")).thenReturn(Optional.of(order));

        // When & Then
        mockMvc.perform(get("/api/orders/track/ORD-123456789-ABCD1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.orderNumber").value("ORD-123456789-ABCD1234"))
                .andExpect(jsonPath("$.status").value("SHIPPED"))
                .andExpect(jsonPath("$.trackingNumber").value("TRACK123456"))
                .andExpect(jsonPath("$.trackingTimeline").isArray())
                .andExpect(jsonPath("$.trackingTimeline[0].status").value("Order Placed"))
                .andExpect(jsonPath("$.trackingTimeline[0].completed").value(true));

        verify(orderService).getOrderByOrderNumber("ORD-123456789-ABCD1234");
    }

    @Test
    void trackOrder_NotFound() throws Exception {
        // Given
        when(orderService.getOrderByOrderNumber("ORD-123456789-ABCD1234")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/orders/track/ORD-123456789-ABCD1234"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Order not found"));
    }

    @Test
    void trackOrderById_Success() throws Exception {
        // Given
        order.setStatus(Order.OrderStatus.DELIVERED);
        when(orderService.getOrderById(1L)).thenReturn(Optional.of(order));

        // When & Then
        mockMvc.perform(get("/api/orders/1/track"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.order.id").value(1))
                .andExpect(jsonPath("$.order.status").value("DELIVERED"))
                .andExpect(jsonPath("$.trackingTimeline").isArray())
                .andExpect(jsonPath("$.trackingTimeline[4].status").value("Delivered"))
                .andExpect(jsonPath("$.trackingTimeline[4].completed").value(true));

        verify(orderService).getOrderById(1L);
    }

    @Test
    void trackOrderById_NotFound() throws Exception {
        // Given
        when(orderService.getOrderById(1L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/orders/1/track"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Order not found"));
    }

    @Test
    void updateTrackingNumber_Success() throws Exception {
        // Given
        order.setTrackingNumber("TRACK789012");
        order.setStatus(Order.OrderStatus.SHIPPED);
        when(orderService.updateTrackingNumber(1L, "TRACK789012")).thenReturn(order);

        // When & Then
        mockMvc.perform(put("/api/orders/1/tracking")
                .param("trackingNumber", "TRACK789012"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tracking number updated successfully"))
                .andExpect(jsonPath("$.order.trackingNumber").value("TRACK789012"))
                .andExpect(jsonPath("$.order.status").value("SHIPPED"));

        verify(orderService).updateTrackingNumber(1L, "TRACK789012");
    }

    @Test
    void updateTrackingNumber_OrderNotFound() throws Exception {
        // Given
        when(orderService.updateTrackingNumber(1L, "TRACK789012"))
                .thenThrow(new RuntimeException("Order not found with id: 1"));

        // When & Then
        mockMvc.perform(put("/api/orders/1/tracking")
                .param("trackingNumber", "TRACK789012"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Order not found with id: 1"));
    }

    @Test
    void trackingTimeline_CorrectProgression() throws Exception {
        // Given - Order in PROCESSING status
        order.setStatus(Order.OrderStatus.PROCESSING);
        when(orderService.getOrderByOrderNumber("ORD-123456789-ABCD1234")).thenReturn(Optional.of(order));

        // When & Then
        mockMvc.perform(get("/api/orders/track/ORD-123456789-ABCD1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingTimeline").isArray())
                .andExpect(jsonPath("$.trackingTimeline[0].status").value("Order Placed"))
                .andExpect(jsonPath("$.trackingTimeline[0].completed").value(true))
                .andExpect(jsonPath("$.trackingTimeline[1].status").value("Order Confirmed"))
                .andExpect(jsonPath("$.trackingTimeline[1].completed").value(true))
                .andExpect(jsonPath("$.trackingTimeline[2].status").value("Processing"))
                .andExpect(jsonPath("$.trackingTimeline[2].completed").value(true))
                .andExpect(jsonPath("$.trackingTimeline[3].status").value("Shipped"))
                .andExpect(jsonPath("$.trackingTimeline[3].completed").value(false))
                .andExpect(jsonPath("$.trackingTimeline[4].status").value("Delivered"))
                .andExpect(jsonPath("$.trackingTimeline[4].completed").value(false));
    }
}
