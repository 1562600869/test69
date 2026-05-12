package com.example.ordersystem.order.service;

import com.example.ordersystem.order.dto.OrderDetailDTO;
import com.example.ordersystem.order.entity.Order;
import com.example.ordersystem.order.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    
    Page<OrderDetailDTO> getAllOrders(Pageable pageable);
    
    Page<OrderDetailDTO> getOrdersByUserId(Long userId, Pageable pageable);
    
    Optional<OrderDetailDTO> getOrderByOrderNo(String orderNo);
    
    Optional<OrderDetailDTO> getOrderById(Long id);
    
    Order createOrder(Order order, List<OrderItem> orderItems);
    
    Order placeOrder(Long userId, List<OrderItem> orderItems, String shippingAddress, String remark);
    
    Order updateOrderStatus(Long id, Order.OrderStatus status);
    
    void deleteOrder(Long id);
}
