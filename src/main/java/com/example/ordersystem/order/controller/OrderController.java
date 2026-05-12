package com.example.ordersystem.order.controller;

import com.example.ordersystem.order.dto.OrderDetailDTO;
import com.example.ordersystem.order.entity.Order;
import com.example.ordersystem.order.entity.OrderItem;
import com.example.ordersystem.order.service.OrderService;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private final OrderService orderService;
    
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    @GetMapping
    public ResponseEntity<Page<OrderDetailDTO>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(orderService.getAllOrders(pageable));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<OrderDetailDTO>> getOrdersByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId, pageable));
    }
    
    @GetMapping("/order-no/{orderNo}")
    public ResponseEntity<OrderDetailDTO> getOrderByOrderNo(@PathVariable String orderNo) {
        Optional<OrderDetailDTO> order = orderService.getOrderByOrderNo(orderNo);
        return order.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailDTO> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Order> placeOrder(@RequestBody PlaceOrderRequest request) {
        Order order = orderService.placeOrder(
                request.getUserId(),
                request.getOrderItems(),
                request.getShippingAddress(),
                request.getRemark()
        );
        return ResponseEntity.ok(order);
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderDetailDTO> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        Order.OrderStatus targetStatus;
        try {
            targetStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        
        Order updatedOrder = orderService.updateOrderStatus(id, targetStatus);
        return orderService.getOrderById(updatedOrder.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
    
    @Data
    public static class PlaceOrderRequest {
        private Long userId;
        private List<OrderItem> orderItems;
        private String shippingAddress;
        private String remark;
    }
}
