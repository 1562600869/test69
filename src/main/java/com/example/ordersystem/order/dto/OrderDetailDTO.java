package com.example.ordersystem.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDetailDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String orderNo;
    private Long userId;
    private String username;
    private BigDecimal totalAmount;
    private String status;
    private String shippingAddress;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemDTO> orderItems;
    
    @Data
    public static class OrderItemDTO implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        private Long id;
        private Long productId;
        private String productName;
        private String productCategory;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
