package com.example.ordersystem.order.repository;

import com.example.ordersystem.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderNo(String orderNo);
    
    Page<Order> findByUserId(Long userId, Pageable pageable);
    
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.orderNo = :orderNo")
    Optional<Order> findByOrderNoWithItems(@Param("orderNo") String orderNo);
    
    @Query(value = "SELECT o FROM Order o LEFT JOIN FETCH o.orderItems ORDER BY o.createdAt DESC",
           countQuery = "SELECT COUNT(o) FROM Order o")
    Page<Order> findAllWithItems(Pageable pageable);
    
    @Query(value = "SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.userId = :userId ORDER BY o.createdAt DESC",
           countQuery = "SELECT COUNT(o) FROM Order o WHERE o.userId = :userId")
    Page<Order> findByUserIdWithItems(@Param("userId") Long userId, Pageable pageable);
    
    @Modifying
    @Query("UPDATE Order o SET o.status = :targetStatus WHERE o.id = :orderId AND o.status = :currentStatus")
    int updateStatusWithCondition(
            @Param("orderId") Long orderId,
            @Param("targetStatus") Order.OrderStatus targetStatus,
            @Param("currentStatus") Order.OrderStatus currentStatus
    );
}
