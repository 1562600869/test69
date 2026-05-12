package com.example.ordersystem.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AlertService {

    public void sendAsyncExceptionAlert(String methodName, Object[] params, Throwable ex) {
        log.error("【告警】异步任务执行异常 - method={}, params={}, error={}", 
                methodName, params, ex.getMessage(), ex);
        
        log.error("【告警】正在发送告警通知...");
        
        log.error("【告警详情】\n" +
                "========================================\n" +
                "告警类型: 异步任务异常\n" +
                "方法名: {}\n" +
                "参数: {}\n" +
                "异常信息: {}\n" +
                "异常类型: {}\n" +
                "========================================",
                methodName, params, ex.getMessage(), ex.getClass().getName());
    }

    public void sendInventoryAlert(String productId, Integer quantity, int retryCount) {
        log.error("【告警】库存扣减失败 - productId={}, requestedQuantity={}, retryCount={}", 
                productId, quantity, retryCount);
    }

    public void sendOrderStatusConflictAlert(Long orderId, String expectedStatus, String actualStatus) {
        log.error("【告警】订单状态并发冲突 - orderId={}, expectedStatus={}, actualStatus={}", 
                orderId, expectedStatus, actualStatus);
    }
}
