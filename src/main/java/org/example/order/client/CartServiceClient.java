package org.example.order.client;

import org.example.order.dto.CartDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "cart-service")
public interface CartServiceClient {
    
    @GetMapping("/api/cart/{userId}/checkout")
    CartDto getCartForCheckout(@PathVariable Long userId);
}
