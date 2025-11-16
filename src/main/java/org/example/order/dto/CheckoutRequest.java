package org.example.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CheckoutRequest {
    
    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;
    
    @NotBlank(message = "Billing address is required")
    private String billingAddress;
    
    @NotBlank(message = "Payment method is required")
    private String paymentMethod;
    
    @NotNull(message = "Card details are required")
    private CardDetails cardDetails;
    
    // Constructors
    public CheckoutRequest() {}
    
    public CheckoutRequest(String shippingAddress, String billingAddress, 
                          String paymentMethod, CardDetails cardDetails) {
        this.shippingAddress = shippingAddress;
        this.billingAddress = billingAddress;
        this.paymentMethod = paymentMethod;
        this.cardDetails = cardDetails;
    }
    
    // Getters and Setters
    public String getShippingAddress() {
        return shippingAddress;
    }
    
    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
    
    public String getBillingAddress() {
        return billingAddress;
    }
    
    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public CardDetails getCardDetails() {
        return cardDetails;
    }
    
    public void setCardDetails(CardDetails cardDetails) {
        this.cardDetails = cardDetails;
    }
    
    // Inner class for card details
    public static class CardDetails {
        @NotBlank(message = "Card number is required")
        private String cardNumber;
        
        @NotBlank(message = "Card holder name is required")
        private String cardHolderName;
        
        @NotBlank(message = "Expiry month is required")
        private String expiryMonth;
        
        @NotBlank(message = "Expiry year is required")
        private String expiryYear;
        
        @NotBlank(message = "CVV is required")
        private String cvv;
        
        // Constructors
        public CardDetails() {}
        
        public CardDetails(String cardNumber, String cardHolderName, 
                          String expiryMonth, String expiryYear, String cvv) {
            this.cardNumber = cardNumber;
            this.cardHolderName = cardHolderName;
            this.expiryMonth = expiryMonth;
            this.expiryYear = expiryYear;
            this.cvv = cvv;
        }
        
        // Getters and Setters
        public String getCardNumber() {
            return cardNumber;
        }
        
        public void setCardNumber(String cardNumber) {
            this.cardNumber = cardNumber;
        }
        
        public String getCardHolderName() {
            return cardHolderName;
        }
        
        public void setCardHolderName(String cardHolderName) {
            this.cardHolderName = cardHolderName;
        }
        
        public String getExpiryMonth() {
            return expiryMonth;
        }
        
        public void setExpiryMonth(String expiryMonth) {
            this.expiryMonth = expiryMonth;
        }
        
        public String getExpiryYear() {
            return expiryYear;
        }
        
        public void setExpiryYear(String expiryYear) {
            this.expiryYear = expiryYear;
        }
        
        public String getCvv() {
            return cvv;
        }
        
        public void setCvv(String cvv) {
            this.cvv = cvv;
        }
    }
}
