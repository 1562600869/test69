package com.example.ordersystem.service;

import com.example.ordersystem.product.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class InventoryService {

    private static final int MAX_RETRY_TIMES = 3;

    private final ProductRepository productRepository;

    public InventoryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    @Retryable(
            retryFor = { RuntimeException.class },
            maxAttempts = MAX_RETRY_TIMES,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public boolean decreaseStock(Long productId, Integer quantity) {
        return decreaseStockWithRetry(productId, quantity, 1);
    }

    @Transactional
    public boolean decreaseStockWithRetry(Long productId, Integer quantity, int attempt) {
        log.info("尝试扣减库存: productId={}, quantity={}, 第{}次尝试", productId, quantity, attempt);

        int updated = productRepository.decreaseStock(productId, quantity);

        if (updated == 1) {
            log.info("库存扣减成功: productId={}, quantity={}, 第{}次尝试成功", productId, quantity, attempt);
            return true;
        }

        if (attempt >= MAX_RETRY_TIMES) {
            log.error("库存扣减失败: productId={}, quantity={}, 已重试{}次，返回库存不足", 
                    productId, quantity, MAX_RETRY_TIMES);
            return false;
        }

        log.warn("库存扣减失败，准备重试: productId={}, quantity={}, 第{}次尝试失败", 
                productId, quantity, attempt);

        try {
            Thread.sleep(100 * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("重试等待被中断: productId={}", productId);
        }

        return decreaseStockWithRetry(productId, quantity, attempt + 1);
    }

    @Transactional
    public void increaseStock(Long productId, Integer quantity) {
        log.info("增加库存: productId={}, quantity={}", productId, quantity);
        productRepository.increaseStock(productId, quantity);
    }
}
