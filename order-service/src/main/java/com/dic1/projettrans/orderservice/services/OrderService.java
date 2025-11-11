package com.dic1.projettrans.orderservice.services;

import com.dic1.projettrans.orderservice.dto.CreateOrderDTO;
import com.dic1.projettrans.orderservice.dto.OrderDTO;
import com.dic1.projettrans.orderservice.entities.OrderStatus;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    OrderDTO create(CreateOrderDTO dto);
    Optional<OrderDTO> getById(String id);
    List<OrderDTO> getAll();
    List<OrderDTO> listByUser(String userId);
    List<OrderDTO> listByStatus(OrderStatus status);
    Optional<OrderDTO> updateStatus(String id, OrderStatus status);
    boolean delete(String id);
}
