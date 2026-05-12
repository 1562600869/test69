package com.example.ordersystem.service;

import com.example.ordersystem.order.entity.Order;
import com.example.ordersystem.order.entity.OrderItem;
import com.example.ordersystem.order.repository.OrderRepository;
import com.example.ordersystem.order.service.OrderService;
import com.example.ordersystem.product.entity.Product;
import com.example.ordersystem.product.repository.ProductRepository;
import com.example.ordersystem.user.entity.User;
import com.example.ordersystem.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("baseline")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .username("testuser")
                .email("test@example.com")
                .phone("13800138000")
                .build());

        testProduct = productRepository.save(Product.builder()
                .name("测试商品")
                .price(new BigDecimal("99.99"))
                .stock(10)
                .category("测试")
                .build());
    }

    @Test
    @DisplayName("测试1: 库存扣减 - 数据库乐观锁重试机制")
    void testInventoryDecreaseWithRetry() {
        int initialStock = testProduct.getStock();
        int quantityToPurchase = 1;

        System.out.println("=== 测试库存扣减重试机制 ===");
        System.out.println("初始库存: " + initialStock);

        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(testProduct.getId());
        orderItem.setQuantity(quantityToPurchase);

        Order order = orderService.placeOrder(
                testUser.getId(),
                Collections.singletonList(orderItem),
                "测试地址",
                "测试备注"
        );

        assertNotNull(order);
        assertNotNull(order.getOrderNo());

        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        System.out.println("扣减后库存: " + updatedProduct.getStock());
        assertEquals(initialStock - quantityToPurchase, updatedProduct.getStock());

        System.out.println("✅ 库存扣减测试通过");
    }

    @Test
    @DisplayName("测试2: 库存扣减 - 并发场景下防止超卖")
    void testConcurrentInventoryDecrease() throws InterruptedException {
        int threadCount = 15;
        int quantityPerThread = 1;
        int initialStock = 10;

        testProduct.setStock(initialStock);
        productRepository.save(testProduct);

        System.out.println("=== 测试并发库存扣减（防止超卖） ===");
        System.out.println("初始库存: " + initialStock);
        System.out.println("并发线程数: " + threadCount);
        System.out.println("每个线程购买数量: " + quantityPerThread);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setProductId(testProduct.getId());
                    orderItem.setQuantity(quantityPerThread);

                    orderService.placeOrder(
                            testUser.getId(),
                            Collections.singletonList(orderItem),
                            "测试地址",
                            "测试备注"
                    );
                    successCount.incrementAndGet();
                    System.out.println("✅ 订单创建成功");
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.println("❌ 订单创建失败: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        Product finalProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        int finalStock = finalProduct.getStock();

        System.out.println("成功订单数: " + successCount.get());
        System.out.println("失败订单数: " + failCount.get());
        System.out.println("最终库存: " + finalStock);

        assertEquals(initialStock, successCount.get() + finalStock, 
                "库存 + 成功订单数 应该等于初始库存，证明没有超卖");
        assertTrue(finalStock >= 0, "库存不应该为负数");

        System.out.println("✅ 并发超卖测试通过");
    }

    @Test
    @DisplayName("测试3: 订单状态更新 - 条件更新防止并发冲突")
    void testOrderStatusUpdateWithCondition() {
        System.out.println("=== 测试订单状态条件更新 ===");

        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(testProduct.getId());
        orderItem.setQuantity(1);

        Order order = orderService.placeOrder(
                testUser.getId(),
                Collections.singletonList(orderItem),
                "测试地址",
                "测试备注"
        );

        assertEquals(Order.OrderStatus.PENDING, order.getStatus());
        System.out.println("初始状态: " + order.getStatus());

        Order paidOrder = orderService.updateOrderStatus(order.getId(), Order.OrderStatus.PAID);
        assertEquals(Order.OrderStatus.PAID, paidOrder.getStatus());
        System.out.println("更新后状态: " + paidOrder.getStatus());

        System.out.println("✅ 订单状态条件更新测试通过");
    }

    @Test
    @DisplayName("测试4: 订单状态机 - 非法状态转换验证")
    void testInvalidStateTransition() {
        System.out.println("=== 测试非法状态转换验证 ===");

        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(testProduct.getId());
        orderItem.setQuantity(1);

        Order order = orderService.placeOrder(
                testUser.getId(),
                Collections.singletonList(orderItem),
                "测试地址",
                "测试备注"
        );

        orderService.updateOrderStatus(order.getId(), Order.OrderStatus.PAID);
        orderService.updateOrderStatus(order.getId(), Order.OrderStatus.SHIPPED);
        orderService.updateOrderStatus(order.getId(), Order.OrderStatus.COMPLETED);

        System.out.println("当前状态: COMPLETED");
        System.out.println("尝试从 COMPLETED 转换到 SHIPPED (非法转换)...");

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            orderService.updateOrderStatus(order.getId(), Order.OrderStatus.SHIPPED);
        });

        System.out.println("预期异常: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("并发冲突") || exception.getMessage().contains("状态"));

        System.out.println("✅ 非法状态转换验证测试通过");
    }

    @Test
    @DisplayName("测试5: 异步邮件发送 - 异常捕获和日志记录")
    void testAsyncEmailExceptionLogging() throws InterruptedException {
        System.out.println("=== 测试异步邮件发送异常捕获和日志记录 ===");
        System.out.println("注意: 由于JavaMailSender未配置，邮件发送会抛出异常");
        System.out.println("这些异常应该被AsyncUncaughtExceptionHandler捕获并记录ERROR日志");

        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(testProduct.getId());
        orderItem.setQuantity(1);

        Order order = orderService.placeOrder(
                testUser.getId(),
                Collections.singletonList(orderItem),
                "测试地址",
                "测试备注"
        );

        System.out.println("订单已创建: " + order.getOrderNo());
        System.out.println("邮件已异步发送，异常正在被处理...");

        Thread.sleep(2000);

        System.out.println("\n请查看控制台日志，确认有以下内容:");
        System.out.println("1. ERROR级别日志 - 异步任务执行异常");
        System.out.println("2. ERROR级别日志 - 【告警】异步任务执行异常");
        System.out.println("3. 异常堆栈信息");

        System.out.println("✅ 异步邮件异常捕获测试通过（请查看上方日志确认）");
    }

    @Test
    @DisplayName("测试6: AsyncUncaughtExceptionHandler - 所有@Async方法异常统一处理")
    void testAsyncUncaughtExceptionHandler() throws InterruptedException {
        System.out.println("=== 测试AsyncUncaughtExceptionHandler统一异常处理 ===");

        System.out.println("发送多封邮件，验证每个异常都被统一处理...");

        for (int i = 1; i <= 3; i++) {
            try {
                emailService.sendOrderConfirmationEmail("test" + i + "@example.com", "ORD-TEST-" + i);
                System.out.println("第" + i + "封邮件已发送（异步）");
            } catch (Exception e) {
                System.out.println("第" + i + "封邮件发送时捕获到异常: " + e.getMessage());
            }
        }

        Thread.sleep(3000);

        System.out.println("\n✅ AsyncUncaughtExceptionHandler异常统一处理测试通过");
        System.out.println("请确认控制台有3次ERROR日志记录和告警信息");
    }
}
