package com.dic1.projettrans.orderservice.controllers;

import com.dic1.projettrans.orderservice.dto.CreateOrderDTO;
import com.dic1.projettrans.orderservice.dto.OrderDTO;
import com.dic1.projettrans.orderservice.dto.UpdateOrderStatusDTO;
import com.dic1.projettrans.orderservice.entities.OrderStatus;
import com.dic1.projettrans.orderservice.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDTO> create(@RequestBody CreateOrderDTO dto) {
        OrderDTO created = orderService.create(dto);
        return ResponseEntity.created(URI.create("/api/orders/" + created.getId())).body(created);
        }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getById(@PathVariable String id) {
        return orderService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAll() {
        return ResponseEntity.ok(orderService.getAll());
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<List<OrderDTO>> listByUser(@PathVariable String userId) {
        return ResponseEntity.ok(orderService.listByUser(userId));
    }

    @GetMapping("/by-status")
    public ResponseEntity<List<OrderDTO>> listByStatus(@RequestParam("value") OrderStatus status) {
        return ResponseEntity.ok(orderService.listByStatus(status));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderDTO> updateStatus(@PathVariable String id, @RequestBody UpdateOrderStatusDTO dto) {
        return orderService.updateStatus(id, dto.getStatus())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        boolean deleted = orderService.delete(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
