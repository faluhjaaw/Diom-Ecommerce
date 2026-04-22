package com.dic1.projettrans.orderservice.controllers;

import com.dic1.projettrans.orderservice.dto.CreateOrderDTO;
import com.dic1.projettrans.orderservice.dto.OrderDTO;
import com.dic1.projettrans.orderservice.dto.UpdateOrderStatusDTO;
import com.dic1.projettrans.orderservice.entities.OrderStatus;
import com.dic1.projettrans.orderservice.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<OrderDTO> create(@RequestBody CreateOrderDTO dto) {
        OrderDTO created = orderService.create(dto);
        return ResponseEntity.created(URI.create("/api/orders/" + created.getId())).body(created);
        }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDEUR', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getById(@PathVariable String id) {
        return orderService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAll() {
        return ResponseEntity.ok(orderService.getAll());
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<List<OrderDTO>> listByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.listByUser(userId));
    }

    @PreAuthorize("hasAnyRole('VENDEUR', 'ADMIN')")
    @GetMapping("/by-status")
    public ResponseEntity<List<OrderDTO>> listByStatus(@RequestParam("value") OrderStatus status) {
        return ResponseEntity.ok(orderService.listByStatus(status));
    }

    @PreAuthorize("hasAnyRole('VENDEUR', 'ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderDTO> updateStatus(@PathVariable String id, @RequestBody UpdateOrderStatusDTO dto) {
        return orderService.updateStatus(id, dto.getStatus())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        boolean deleted = orderService.delete(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @GetMapping("/check")
    public ResponseEntity<Boolean> checkUserOrder(@RequestParam Long userId, @RequestParam String productId) {
        return ResponseEntity.ok(orderService.hasUserOrderedProduct(userId, productId));
    }

    @PreAuthorize("hasAnyRole('VENDEUR', 'ADMIN')")
    @GetMapping("/vendor/{vendorId}")
    public ResponseEntity<List<OrderDTO>> listByVendor(@PathVariable Long vendorId) {
        return ResponseEntity.ok(orderService.listByVendor(vendorId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(orderService.getStats());
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @PostMapping("/from-cart/{cartId}")
    public ResponseEntity<OrderDTO> createFromCart(@PathVariable String cartId, @RequestBody Map<String, String> body) {
        Long userId = Long.parseLong(body.get("userId"));
        String paymentMethod = body.get("paymentMethod");
        OrderDTO created = orderService.createFromCart(cartId, userId, paymentMethod);
        return ResponseEntity.created(URI.create("/api/orders/" + created.getId())).body(created);
    }
}
