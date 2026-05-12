package com.example.ordersystem.service;

import com.example.ordersystem.order.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OrderStateMachineService {

    private static final Map<Order.OrderStatus, List<Order.OrderStatus>> VALID_TRANSITIONS = new HashMap<>();

    static {
        VALID_TRANSITIONS.put(Order.OrderStatus.PENDING, 
                Arrays.asList(Order.OrderStatus.PAID, Order.OrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(Order.OrderStatus.PAID, 
                Arrays.asList(Order.OrderStatus.SHIPPED, Order.OrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(Order.OrderStatus.SHIPPED, 
                Arrays.asList(Order.OrderStatus.COMPLETED));
        VALID_TRANSITIONS.put(Order.OrderStatus.COMPLETED, 
                Arrays.asList());
        VALID_TRANSITIONS.put(Order.OrderStatus.CANCELLED, 
                Arrays.asList());
    }

    public boolean isValidTransition(Order.OrderStatus currentStatus, Order.OrderStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            log.warn("状态转换验证失败: 当前状态或目标状态为空");
            return false;
        }

        if (currentStatus == targetStatus) {
            log.debug("状态相同，无需转换: status={}", currentStatus);
            return true;
        }

        List<Order.OrderStatus> validNextStates = VALID_TRANSITIONS.get(currentStatus);
        if (validNextStates == null) {
            log.error("未知的当前状态: {}", currentStatus);
            return false;
        }

        boolean isValid = validNextStates.contains(targetStatus);
        if (!isValid) {
            log.warn("非法的状态转换: current={}, target={}", currentStatus, targetStatus);
        }

        return isValid;
    }

    public void validateTransition(Order.OrderStatus currentStatus, Order.OrderStatus targetStatus) {
        if (!isValidTransition(currentStatus, targetStatus)) {
            throw new IllegalStateException(
                    String.format("非法的订单状态转换: 从 %s 到 %s", currentStatus, targetStatus));
        }
    }

    public boolean isFinalState(Order.OrderStatus status) {
        return status == Order.OrderStatus.COMPLETED || status == Order.OrderStatus.CANCELLED;
    }
}
