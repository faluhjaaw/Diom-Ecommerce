package com.dic1.projettrans.cartservice.controllers;

import com.dic1.projettrans.cartservice.dto.AddItemDTO;
import com.dic1.projettrans.cartservice.dto.CartDTO;
import com.dic1.projettrans.cartservice.dto.UpdateItemQuantityDTO;
import com.dic1.projettrans.cartservice.services.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // Get or create the cart for a user
    @GetMapping("/{userId}")
    public ResponseEntity<CartDTO> getOrCreate(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getOrCreateCart(userId));
    }

    // Add item to cart
    @PostMapping("/{userId}/items")
    public ResponseEntity<CartDTO> addItem(@PathVariable Long userId, @RequestBody AddItemDTO dto) {
        CartDTO updated = cartService.addItem(userId, dto);
        return ResponseEntity.created(URI.create("/api/carts/" + userId)).body(updated);
    }

    // Update item quantity
    @PutMapping("/{userId}/items")
    public ResponseEntity<CartDTO> updateItemQuantity(@PathVariable Long userId, @RequestBody UpdateItemQuantityDTO dto) {
        return ResponseEntity.ok(cartService.updateItemQuantity(userId, dto));
    }

    // Remove an item
    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<CartDTO> removeItem(@PathVariable Long userId, @PathVariable String productId) {
        return ResponseEntity.ok(cartService.removeItem(userId, productId));
    }

    // Clear the cart
    @DeleteMapping("/{userId}")
    public ResponseEntity<CartDTO> clear(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.clearCart(userId));
    }

    // Endpoints internes pour les appels entre services
    @GetMapping("/internal/{userId}")
    public ResponseEntity<CartDTO> getOrCreateInternal(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getOrCreateCart(userId));
    }

    @DeleteMapping("/internal/{userId}")
    public ResponseEntity<CartDTO> clearInternal(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.clearCart(userId));
    }
}
