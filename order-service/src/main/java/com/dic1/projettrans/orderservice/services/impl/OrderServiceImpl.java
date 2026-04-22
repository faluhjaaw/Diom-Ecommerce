package com.dic1.projettrans.orderservice.services.impl;

import com.dic1.projettrans.orderservice.dto.CreateOrderDTO;
import com.dic1.projettrans.orderservice.dto.OrderDTO;
import com.dic1.projettrans.orderservice.dto.OrderItemDTO;
import com.dic1.projettrans.orderservice.entities.Order;
import com.dic1.projettrans.orderservice.entities.OrderStatus;
import com.dic1.projettrans.orderservice.feign.CartServiceRestClient;
import com.dic1.projettrans.orderservice.feign.CustomerServiceRestClient;
import com.dic1.projettrans.orderservice.feign.ProductServiceRestClient;
import com.dic1.projettrans.orderservice.model.Cart;
import com.dic1.projettrans.orderservice.model.Product;
import com.dic1.projettrans.orderservice.repositories.OrderRepository;
import com.dic1.projettrans.orderservice.events.OrderCreatedEvent;
import com.dic1.projettrans.orderservice.kafka.OrderEventProducer;
import com.dic1.projettrans.orderservice.model.Customer;
import com.dic1.projettrans.orderservice.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceRestClient productClient;
    private final CustomerServiceRestClient customerClient;
    private final CartServiceRestClient cartClient;
    private final OrderEventProducer orderEventProducer;

    @Override
    public OrderDTO create(CreateOrderDTO dto) {
        // Récupérer le client une seule fois (validation + adresse)
        Customer customer;
        try {
            customer = customerClient.findUserById(dto.getUserId());
            if (customer == null) throw new IllegalArgumentException("Client introuvable : " + dto.getUserId());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Client introuvable : " + dto.getUserId());
        }

        // Map and validate items
        List<OrderItemDTO> incomingItems = dto.getItems() == null ? List.of() : dto.getItems();
        List<Order.OrderItem> items = new ArrayList<>();
        for (OrderItemDTO i : incomingItems) {
            requirePositiveQuantity(i.getQuantity());
            Product product = null;
            try {
                product = productClient.findProductById(i.getProductId());
            } catch (Exception ignored) {}
            if (product == null) {
                throw new IllegalArgumentException("Produit introuvable : " + i.getProductId());
            }
            if (i.getUnitPrice() == null) {
                i.setUnitPrice(product.getPrice());
            }
            requirePrice(i.getUnitPrice());
            items.add(Order.OrderItem.builder()
                    .productId(i.getProductId())
                    .vendorId(product.getVendorId())
                    .quantity(i.getQuantity())
                    .unitPrice(i.getUnitPrice())
                    .build());
        }

        BigDecimal total = items.stream()
                .map(it -> it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .userId(dto.getUserId())
                .items(items)
                .total(total)
                .shippingAddress(customer.getAdresse())
                .paymentMethod(dto.getPaymentMethod())
                .status(OrderStatus.CREATED)
                .build();
        Order saved = orderRepository.save(order);

        for (Order.OrderItem item : saved.getItems()) {
            try {
                productClient.decrementStock(item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                orderRepository.deleteById(saved.getId());
                throw new IllegalStateException("Stock insuffisant pour le produit "
                        + item.getProductId() + " : " + e.getMessage());
            }
        }

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
    public List<OrderDTO> listByUser(Long userId) {
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

    @Override
    public boolean hasUserOrderedProduct(Long userId, String productId) {
        return orderRepository.existsByUserIdAndItems_ProductId(userId, productId);
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
                                .vendorId(i.getVendorId())
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

    @Override
    public List<OrderDTO> listByVendor(Long vendorId) {
        return orderRepository.findByItems_VendorId(vendorId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public Map<String, Long> getStats() {
        return orderRepository.findAll().stream()
                .collect(Collectors.groupingBy(o -> o.getStatus().name(), Collectors.counting()));
    }

    @Override
    public OrderDTO createFromCart(String cartId, Long userId, String paymentMethod) {
        Cart cart;
        try {
            cart = cartClient.getCart(userId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Impossible de récupérer le panier pour l'utilisateur : " + userId);
        }

        if (cart == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Le panier est vide");
        }

        // Récupérer le client une seule fois (validation + adresse)
        Customer customer;
        try {
            customer = customerClient.findUserById(userId);
            if (customer == null) throw new IllegalArgumentException("Client introuvable : " + userId);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Client introuvable : " + userId);
        }

        List<Order.OrderItem> orderItems = cart.getItems().stream()
                .map(cartItem -> {
                    Product product = null;
                    try {
                        product = productClient.findProductById(cartItem.getProductId());
                    } catch (Exception ignored) {}
                    Long vendorId = (product != null) ? product.getVendorId() : null;
                    return Order.OrderItem.builder()
                            .productId(cartItem.getProductId())
                            .vendorId(vendorId)
                            .quantity(cartItem.getQuantity())
                            .unitPrice(cartItem.getUnitPrice())
                            .build();
                })
                .collect(Collectors.toList());

        Order order = Order.builder()
                .userId(userId)
                .items(orderItems)
                .total(cart.getTotal())
                .shippingAddress(customer.getAdresse())
                .paymentMethod(paymentMethod)
                .status(OrderStatus.CREATED)
                .build();

        Order saved = orderRepository.save(order);

        for (Order.OrderItem item : saved.getItems()) {
            try {
                productClient.decrementStock(item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                orderRepository.deleteById(saved.getId());
                throw new IllegalStateException("Stock insuffisant pour le produit "
                        + item.getProductId() + " : " + e.getMessage());
            }
        }

        // Publier l'événement Kafka — cart-service videra le panier de façon asynchrone
        orderEventProducer.publishOrderCreated(
                OrderCreatedEvent.builder().orderId(saved.getId()).userId(userId).build()
        );
        return toDTO(saved);
    }

}
