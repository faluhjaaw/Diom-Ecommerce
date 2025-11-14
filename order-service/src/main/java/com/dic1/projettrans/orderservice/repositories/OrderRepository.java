package com.dic1.projettrans.orderservice.repositories;

import com.dic1.projettrans.orderservice.entities.Order;
import com.dic1.projettrans.orderservice.entities.OrderStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByUserId(Long userId);
    List<Order> findByStatus(OrderStatus status);
    boolean existsByUserIdAndItems_ProductId(Long userId, String productId);
}
