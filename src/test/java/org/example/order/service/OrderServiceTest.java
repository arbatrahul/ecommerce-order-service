package org.example.order.service;

import org.example.order.client.CartServiceClient;
import org.example.order.dto.CartDto;
import org.example.order.dto.CartItemDto;
import org.example.order.dto.CheckoutRequest;
import org.example.order.entity.Order;
import org.example.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartServiceClient cartServiceClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderService orderService;

    private Order order;
    private CartDto cartDto;
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

        // Setup Cart
        CartItemDto cartItem = new CartItemDto();
        cartItem.setProductId(1L);
        cartItem.setProductName("Test Product");
        cartItem.setPrice(new BigDecimal("49.99"));
        cartItem.setQuantity(2);

        cartDto = new CartDto();
        cartDto.setUserId(1L);
        cartDto.setItems(Arrays.asList(cartItem));
        cartDto.setTotalAmount(new BigDecimal("99.98"));
        cartDto.setTotalItems(2);

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
    void createOrder_Success() {
        // Given
        when(cartServiceClient.getCartForCheckout(1L)).thenReturn(cartDto);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        Order result = orderService.createOrder(1L, checkoutRequest);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("123 Test St, Test City, TC 12345", result.getShippingAddress());
        assertEquals("123 Test St, Test City, TC 12345", result.getBillingAddress());
        verify(orderRepository).save(any(Order.class));
        verify(kafkaTemplate).send(eq("order-events"), eq("order-created"), any());
    }

    @Test
    void createOrder_EmptyCart() {
        // Given
        cartDto.setItems(Arrays.asList()); // Empty cart
        when(cartServiceClient.getCartForCheckout(1L)).thenReturn(cartDto);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> orderService.createOrder(1L, checkoutRequest));
        assertEquals("Cart is empty", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getOrderById_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When
        Optional<Order> result = orderService.getOrderById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(order, result.get());
        verify(orderRepository).findById(1L);
    }

    @Test
    void getOrderById_NotFound() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        Optional<Order> result = orderService.getOrderById(1L);

        // Then
        assertFalse(result.isPresent());
        verify(orderRepository).findById(1L);
    }

    @Test
    void getOrderByOrderNumber_Success() {
        // Given
        when(orderRepository.findByOrderNumber("ORD-123456789-ABCD1234")).thenReturn(Optional.of(order));

        // When
        Optional<Order> result = orderService.getOrderByOrderNumber("ORD-123456789-ABCD1234");

        // Then
        assertTrue(result.isPresent());
        assertEquals(order, result.get());
        verify(orderRepository).findByOrderNumber("ORD-123456789-ABCD1234");
    }

    @Test
    void getOrdersByUserId_Success() {
        // Given
        List<Order> orders = Arrays.asList(order);
        Page<Order> orderPage = new PageImpl<>(orders);
        Pageable pageable = PageRequest.of(0, 10);
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(orderPage);

        // When
        Page<Order> result = orderService.getOrdersByUserId(1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(order, result.getContent().get(0));
        verify(orderRepository).findByUserIdOrderByCreatedAtDesc(1L, pageable);
    }

    @Test
    void updateOrderStatus_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        Order result = orderService.updateOrderStatus(1L, Order.OrderStatus.CONFIRMED);

        // Then
        assertNotNull(result);
        assertEquals(Order.OrderStatus.CONFIRMED, result.getStatus());
        verify(orderRepository).save(any(Order.class));
        verify(kafkaTemplate).send(eq("order-events"), eq("order-status-updated"), any());
    }

    @Test
    void updateOrderStatus_OrderNotFound() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> orderService.updateOrderStatus(1L, Order.OrderStatus.CONFIRMED));
        assertEquals("Order not found with id: 1", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateTrackingNumber_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        Order result = orderService.updateTrackingNumber(1L, "TRACK123456");

        // Then
        assertNotNull(result);
        assertEquals("TRACK123456", result.getTrackingNumber());
        assertEquals(Order.OrderStatus.SHIPPED, result.getStatus()); // Should auto-update to SHIPPED
        verify(orderRepository).save(any(Order.class));
        verify(kafkaTemplate).send(eq("order-events"), eq("tracking-updated"), any());
    }

    @Test
    void updateTrackingNumber_OrderNotFound() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> orderService.updateTrackingNumber(1L, "TRACK123456"));
        assertEquals("Order not found with id: 1", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateTrackingNumber_EmptyTrackingNumber() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        Order result = orderService.updateTrackingNumber(1L, "");

        // Then
        assertNotNull(result);
        assertEquals("", result.getTrackingNumber());
        assertEquals(Order.OrderStatus.PENDING, result.getStatus()); // Should not change status
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void updatePaymentStatus_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        Order result = orderService.updatePaymentStatus(1L, Order.PaymentStatus.PAID);

        // Then
        assertNotNull(result);
        assertEquals(Order.PaymentStatus.PAID, result.getPaymentStatus());
        assertEquals(Order.OrderStatus.CONFIRMED, result.getStatus()); // Should auto-update to CONFIRMED
        verify(orderRepository).save(any(Order.class));
        verify(kafkaTemplate).send(eq("order-events"), eq("payment-status-updated"), any());
    }

    @Test
    void updatePaymentStatus_Failed() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        Order result = orderService.updatePaymentStatus(1L, Order.PaymentStatus.FAILED);

        // Then
        assertNotNull(result);
        assertEquals(Order.PaymentStatus.FAILED, result.getPaymentStatus());
        assertEquals(Order.OrderStatus.CANCELLED, result.getStatus()); // Should auto-update to CANCELLED
        verify(orderRepository).save(any(Order.class));
        verify(kafkaTemplate).send(eq("order-events"), eq("payment-status-updated"), any());
    }

    @Test
    void updatePaymentStatus_OrderNotFound() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> orderService.updatePaymentStatus(1L, Order.PaymentStatus.PAID));
        assertEquals("Order not found with id: 1", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void calculateTax_Success() {
        // Given
        BigDecimal amount = new BigDecimal("100.00");

        // When
        BigDecimal tax = orderService.calculateTax(amount);

        // Then
        assertEquals(0, new BigDecimal("8.50").compareTo(tax)); // 8.5% tax
    }

    @Test
    void calculateShipping_FreeShipping() {
        // Given
        BigDecimal amount = new BigDecimal("75.00"); // Over $50

        // When
        BigDecimal shipping = orderService.calculateShipping(amount);

        // Then
        assertEquals(BigDecimal.ZERO, shipping);
    }

    @Test
    void calculateShipping_StandardShipping() {
        // Given
        BigDecimal amount = new BigDecimal("25.00"); // Under $50

        // When
        BigDecimal shipping = orderService.calculateShipping(amount);

        // Then
        assertEquals(new BigDecimal("9.99"), shipping);
    }

    @Test
    void generateOrderNumber_UniqueFormat() {
        // When
        String orderNumber = orderService.generateOrderNumber();

        // Then
        assertNotNull(orderNumber);
        assertTrue(orderNumber.startsWith("ORD-"));
        assertTrue(orderNumber.length() > 15); // Should be long enough to be unique
    }
}
