package com.dic1.projettrans.orderservice.services.impl;

import com.dic1.projettrans.orderservice.dto.CreateOrderDTO;
import com.dic1.projettrans.orderservice.dto.OrderDTO;
import com.dic1.projettrans.orderservice.dto.OrderItemDTO;
import com.dic1.projettrans.orderservice.entities.Order;
import com.dic1.projettrans.orderservice.entities.OrderStatus;
import com.dic1.projettrans.orderservice.feign.CustomerServiceRestClient;
import com.dic1.projettrans.orderservice.feign.ProductServiceRestClient;
import com.dic1.projettrans.orderservice.model.Product;
import com.dic1.projettrans.orderservice.repositories.OrderRepository;
import com.dic1.projettrans.orderservice.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceRestClient productClient;
    private final CustomerServiceRestClient customerClient;

    @Override
    public OrderDTO create(CreateOrderDTO dto) {
        // Validate customer exists
        long userIdLong;
        try {
            userIdLong = Long.parseLong(dto.getUserId());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid userId format: " + dto.getUserId());
        }
        try {
            if (customerClient.findUserById(userIdLong) == null) {
                throw new IllegalArgumentException("Customer not found: " + dto.getUserId());
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Customer not found: " + dto.getUserId());
        }

        // Map and validate items
        List<OrderItemDTO> incomingItems = dto.getItems() == null ? List.of() : dto.getItems();
        List<Order.OrderItem> items = new ArrayList<>();
        for (OrderItemDTO i : incomingItems) {
            requirePositiveQuantity(i.getQuantity());
            // Resolve unit price if missing
            if (i.getUnitPrice() == null) {
                Product product = null;
                try {
                    product = productClient.findProductById(i.getProductId());
                } catch (Exception ignored) {}
                if (product == null) {
                    throw new IllegalArgumentException("Product not found: " + i.getProductId());
                }
                i.setUnitPrice(product.getPrice());
            }
            requirePrice(i.getUnitPrice());
            items.add(Order.OrderItem.builder()
                    .productId(i.getProductId())
                    .quantity(i.getQuantity())
                    .unitPrice(i.getUnitPrice())
                    .build());
        }

        // Compute total
        BigDecimal total = items.stream()
                .map(it -> it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity())))
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

    private void requirePositiveQuantity(Integer q) {
        if (q == null || q <= 0) {
            throw new IllegalArgumentException("Quantity must be > 0");
        }
    }

    private void requirePrice(BigDecimal p) {
        if (p == null || p.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price must be >= 0");
        }
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
