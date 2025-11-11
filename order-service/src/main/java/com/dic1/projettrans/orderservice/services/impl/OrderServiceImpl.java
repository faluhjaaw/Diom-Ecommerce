package com.dic1.projettrans.orderservice.services.impl;

import com.dic1.projettrans.orderservice.dto.CreateOrderDTO;
import com.dic1.projettrans.orderservice.dto.OrderDTO;
import com.dic1.projettrans.orderservice.dto.OrderItemDTO;
import com.dic1.projettrans.orderservice.entities.Order;
import com.dic1.projettrans.orderservice.entities.OrderStatus;
import com.dic1.projettrans.orderservice.repositories.OrderRepository;
import com.dic1.projettrans.orderservice.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Override
    public OrderDTO create(CreateOrderDTO dto) {
        // Map items
        List<Order.OrderItem> items = (dto.getItems() == null ? List.<OrderItemDTO>of() : dto.getItems())
                .stream()
                .map(i -> Order.OrderItem.builder()
                        .productId(i.getProductId())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .build())
                .collect(Collectors.toList());
        // Compute total
        BigDecimal total = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Order order = Order.builder()
                .userId(dto.getUserId())
                .items(items)
                .total(total)
                .shippingAddress(dto.getShippingAddress())
                .paymentMethod(dto.getPaymentMethod())
                .status(OrderStatus.CREATED)
                .build();
        Order saved = orderRepository.save(order);
        return toDTO(saved);
    }

    @Override
    public Optional<OrderDTO> getById(String id) {
        return orderRepository.findById(id).map(this::toDTO);
    }

    @Override
    public List<OrderDTO> getAll() {
        return orderRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<OrderDTO> listByUser(String userId) {
        return orderRepository.findByUserId(userId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<OrderDTO> listByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public Optional<OrderDTO> updateStatus(String id, OrderStatus status) {
        return orderRepository.findById(id).map(existing -> {
            existing.setStatus(status);
            return toDTO(orderRepository.save(existing));
        });
    }

    @Override
    public boolean delete(String id) {
        if (!orderRepository.existsById(id)) return false;
        orderRepository.deleteById(id);
        return true;
    }

    private OrderDTO toDTO(Order order) {
        if (order == null) return null;
        return OrderDTO.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .items(order.getItems().stream()
                        .map(i -> OrderItemDTO.builder()
                                .productId(i.getProductId())
                                .quantity(i.getQuantity())
                                .unitPrice(i.getUnitPrice())
                                .build())
                        .collect(Collectors.toList()))
                .total(order.getTotal())
                .status(order.getStatus())
                .shippingAddress(order.getShippingAddress())
                .paymentMethod(order.getPaymentMethod())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
