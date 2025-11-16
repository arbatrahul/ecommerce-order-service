package org.example.order.dto;

import java.math.BigDecimal;
import java.util.List;

public class CartDto {
    private String id;
    private Long userId;
    private List<CartItemDto> items;
    private BigDecimal totalAmount;
    private Integer totalItems;
    
    // Constructors
    public CartDto() {}
    
    public CartDto(String id, Long userId, List<CartItemDto> items, 
                  BigDecimal totalAmount, Integer totalItems) {
        this.id = id;
        this.userId = userId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.totalItems = totalItems;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public List<CartItemDto> getItems() {
        return items;
    }
    
    public void setItems(List<CartItemDto> items) {
        this.items = items;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public Integer getTotalItems() {
        return totalItems;
    }
    
    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }
}
