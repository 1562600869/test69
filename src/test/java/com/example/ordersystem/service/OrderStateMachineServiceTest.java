package com.example.ordersystem.service;

import com.example.ordersystem.order.entity.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStateMachineServiceTest {

    private OrderStateMachineService stateMachineService;

    @BeforeEach
    void setUp() {
        stateMachineService = new OrderStateMachineService();
    }

    @Test
    @DisplayName("测试合法的状态转换 - PENDING -> PAID")
    void testValidTransition_PendingToPaid() {
        assertTrue(stateMachineService.isValidTransition(
                Order.OrderStatus.PENDING, 
                Order.OrderStatus.PAID
        ));
    }

    @Test
    @DisplayName("测试合法的状态转换 - PAID -> SHIPPED")
    void testValidTransition_PaidToShipped() {
        assertTrue(stateMachineService.isValidTransition(
                Order.OrderStatus.PAID, 
                Order.OrderStatus.SHIPPED
        ));
    }

    @Test
    @DisplayName("测试合法的状态转换 - SHIPPED -> COMPLETED")
    void testValidTransition_ShippedToCompleted() {
        assertTrue(stateMachineService.isValidTransition(
                Order.OrderStatus.SHIPPED, 
                Order.OrderStatus.COMPLETED
        ));
    }

    @Test
    @DisplayName("测试合法的状态转换 - PENDING -> CANCELLED")
    void testValidTransition_PendingToCancelled() {
        assertTrue(stateMachineService.isValidTransition(
                Order.OrderStatus.PENDING, 
                Order.OrderStatus.CANCELLED
        ));
    }

    @Test
    @DisplayName("测试非法的状态转换 - COMPLETED -> SHIPPED (已完成订单不能变为配送中)")
    void testInvalidTransition_CompletedToShipped() {
        assertFalse(stateMachineService.isValidTransition(
                Order.OrderStatus.COMPLETED, 
                Order.OrderStatus.SHIPPED
        ));
    }

    @Test
    @DisplayName("测试非法的状态转换 - CANCELLED -> PAID")
    void testInvalidTransition_CancelledToPaid() {
        assertFalse(stateMachineService.isValidTransition(
                Order.OrderStatus.CANCELLED, 
                Order.OrderStatus.PAID
        ));
    }

    @Test
    @DisplayName("测试相同状态转换 - 应该允许")
    void testSameStatusTransition() {
        assertTrue(stateMachineService.isValidTransition(
                Order.OrderStatus.PENDING, 
                Order.OrderStatus.PENDING
        ));
    }

    @Test
    @DisplayName("测试非法状态转换验证 - 应该抛出异常")
    void testValidateInvalidTransition() {
        assertThrows(IllegalStateException.class, () -> {
            stateMachineService.validateTransition(
                    Order.OrderStatus.COMPLETED, 
                    Order.OrderStatus.SHIPPED
            );
        });
    }

    @Test
    @DisplayName("测试终态判断 - COMPLETED是终态")
    void testIsFinalState_Completed() {
        assertTrue(stateMachineService.isFinalState(Order.OrderStatus.COMPLETED));
    }

    @Test
    @DisplayName("测试终态判断 - CANCELLED是终态")
    void testIsFinalState_Cancelled() {
        assertTrue(stateMachineService.isFinalState(Order.OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("测试终态判断 - PENDING不是终态")
    void testIsFinalState_Pending() {
        assertFalse(stateMachineService.isFinalState(Order.OrderStatus.PENDING));
    }
}
