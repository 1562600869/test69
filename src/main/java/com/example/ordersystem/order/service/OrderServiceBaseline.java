package com.example.ordersystem.order.service;

import com.example.ordersystem.order.dto.OrderDetailDTO;
import com.example.ordersystem.order.entity.Order;
import com.example.ordersystem.order.entity.OrderItem;
import com.example.ordersystem.order.repository.OrderItemRepository;
import com.example.ordersystem.order.repository.OrderRepository;
import com.example.ordersystem.product.entity.Product;
import com.example.ordersystem.product.repository.ProductRepository;
import com.example.ordersystem.service.AlertService;
import com.example.ordersystem.service.EmailService;
import com.example.ordersystem.service.InventoryService;
import com.example.ordersystem.service.OrderStateMachineService;
import com.example.ordersystem.user.entity.User;
import com.example.ordersystem.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@Profile("baseline")
public class OrderServiceBaseline implements OrderService {
    
    private static final int MAX_INVENTORY_RETRY = 3;
    
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final OrderStateMachineService orderStateMachineService;
    private final EmailService emailService;
    private final AlertService alertService;
    
    public OrderServiceBaseline(OrderRepository orderRepository,
                               OrderItemRepository orderItemRepository,
                               UserRepository userRepository,
                               ProductRepository productRepository,
                               InventoryService inventoryService,
                               OrderStateMachineService orderStateMachineService,
                               EmailService emailService,
                               AlertService alertService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.orderStateMachineService = orderStateMachineService;
        this.emailService = emailService;
        this.alertService = alertService;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<OrderDetailDTO> getAllOrders(Pageable pageable) {
        Page<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc(pageable);
        return orders.map(this::convertToDTO);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<OrderDetailDTO> getOrdersByUserId(Long userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserId(userId, pageable);
        return orders.map(this::convertToDTO);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<OrderDetailDTO> getOrderByOrderNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo).map(this::convertToDTO);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<OrderDetailDTO> getOrderById(Long id) {
        return orderRepository.findById(id).map(this::convertToDTO);
    }
    
    private OrderDetailDTO convertToDTO(Order order) {
        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setUserId(order.getUserId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus().name());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setRemark(order.getRemark());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        
        User user = userRepository.findById(order.getUserId()).orElse(null);
        if (user != null) {
            dto.setUsername(user.getUsername());
        }
        
        List<OrderDetailDTO.OrderItemDTO> itemDTOs = new ArrayList<>();
        for (OrderItem item : order.getOrderItems()) {
            OrderDetailDTO.OrderItemDTO itemDTO = new OrderDetailDTO.OrderItemDTO();
            itemDTO.setId(item.getId());
            itemDTO.setProductId(item.getProductId());
            itemDTO.setQuantity(item.getQuantity());
            itemDTO.setUnitPrice(item.getUnitPrice());
            itemDTO.setSubtotal(item.getSubtotal());
            
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product != null) {
                itemDTO.setProductName(product.getName());
                itemDTO.setProductCategory(product.getCategory());
            }
            itemDTOs.add(itemDTO);
        }
        dto.setOrderItems(itemDTOs);
        
        return dto;
    }
    
    @Override
    @Transactional
    public Order createOrder(Order order, List<OrderItem> orderItems) {
        Order savedOrder = orderRepository.save(order);
        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
        }
        orderItemRepository.saveAll(orderItems);
        return savedOrder;
    }
    
    @Override
    @Transactional
    public Order placeOrder(Long userId, List<OrderItem> orderItems, String shippingAddress, String remark) {
        log.info("开始创建订单: userId={}, itemCount={}", userId, orderItems.size());
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + userId));
        
        String orderNo = generateOrderNo();
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItem item : orderItems) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("商品不存在: " + item.getProductId()));
            
            if (product.getStock() < item.getQuantity()) {
                log.error("库存不足: productId={}, stock={}, requested={}", 
                        product.getId(), product.getStock(), item.getQuantity());
                throw new RuntimeException("商品库存不足: " + product.getName());
            }
            
            boolean success = inventoryService.decreaseStockWithRetry(product.getId(), item.getQuantity(), 1);
            if (!success) {
                log.error("库存扣减失败，已重试{}次: productId={}, requested={}", 
                        MAX_INVENTORY_RETRY, product.getId(), item.getQuantity());
                alertService.sendInventoryAlert(product.getId().toString(), item.getQuantity(), MAX_INVENTORY_RETRY);
                throw new RuntimeException("库存扣减失败，商品: " + product.getName() + " 库存不足");
            }
            log.info("库存扣减成功: productId={}, quantity={}", product.getId(), item.getQuantity());
            
            item.setUnitPrice(product.getPrice());
            item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            totalAmount = totalAmount.add(item.getSubtotal());
        }
        
        Order order = Order.builder()
                .orderNo(orderNo)
                .userId(userId)
                .totalAmount(totalAmount)
                .status(Order.OrderStatus.PENDING)
                .shippingAddress(shippingAddress)
                .remark(remark)
                .build();
        
        Order savedOrder = orderRepository.save(order);
        log.info("订单创建成功: orderId={}, orderNo={}", savedOrder.getId(), savedOrder.getOrderNo());
        
        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
        }
        orderItemRepository.saveAll(orderItems);
        
        try {
            emailService.sendOrderConfirmationEmail(user.getEmail(), orderNo);
            log.info("订单确认邮件已异步发送: orderNo={}", orderNo);
        } catch (Exception e) {
            log.warn("邮件发送失败，但订单已创建成功: orderNo={}", orderNo);
        }
        
        return savedOrder;
    }
    
    @Override
    @Transactional
    public Order updateOrderStatus(Long id, Order.OrderStatus targetStatus) {
        log.info("更新订单状态: orderId={}, targetStatus={}", id, targetStatus);
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在: " + id));
        
        Order.OrderStatus currentStatus = order.getStatus();
        log.info("订单当前状态: orderId={}, currentStatus={}", id, currentStatus);
        
        if (currentStatus == targetStatus) {
            log.info("订单状态无需变更: orderId={}, status={}", id, targetStatus);
            return order;
        }
        
        orderStateMachineService.validateTransition(currentStatus, targetStatus);
        log.info("状态转换验证通过: orderId={}, {} -> {}", id, currentStatus, targetStatus);
        
        int updatedRows = orderRepository.updateStatusWithCondition(id, targetStatus, currentStatus);
        if (updatedRows != 1) {
            log.error("订单状态更新失败，存在并发冲突: orderId={}, expectedStatus={}", id, currentStatus);
            alertService.sendOrderStatusConflictAlert(id, currentStatus.name(), targetStatus.name());
            throw new IllegalStateException("订单状态更新失败，存在并发冲突，请重试");
        }
        
        log.info("订单状态更新成功: orderId={}, status={}", id, targetStatus);
        
        Order updatedOrder = orderRepository.findById(id).orElse(order);
        
        User user = userRepository.findById(updatedOrder.getUserId()).orElse(null);
        if (user != null) {
            try {
                emailService.sendOrderStatusEmail(user.getEmail(), updatedOrder.getOrderNo(), targetStatus.name());
            } catch (Exception e) {
                log.warn("状态更新邮件发送失败: orderId={}", id);
            }
        }
        
        return updatedOrder;
    }
    
    @Override
    @Transactional
    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }
    
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%06d", new Random().nextInt(1000000));
        return "ORD" + timestamp + random;
    }
}
