package com.dic1.projettrans.cartservice.services.impl;

import com.dic1.projettrans.cartservice.dto.AddItemDTO;
import com.dic1.projettrans.cartservice.dto.CartDTO;
import com.dic1.projettrans.cartservice.dto.CartItemDTO;
import com.dic1.projettrans.cartservice.dto.UpdateItemQuantityDTO;
import com.dic1.projettrans.cartservice.entities.Cart;
import com.dic1.projettrans.cartservice.repositories.CartRepository;
import com.dic1.projettrans.cartservice.services.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;

    @Override
    public CartDTO getOrCreateCart(String userId) {
        return cartRepository.findByUserId(userId)
                .map(this::toDTO)
                .orElseGet(() -> {
                    Cart cart = Cart.builder()
                            .userId(userId)
                            .items(new ArrayList<>())
                            .total(BigDecimal.ZERO)
                            .build();
                    Cart saved = cartRepository.save(cart);
                    return toDTO(saved);
                });
    }

    @Override
    public Optional<CartDTO> getCart(String userId) {
        return cartRepository.findByUserId(userId).map(this::toDTO);
    }

    @Override
    public CartDTO addItem(String userId, AddItemDTO dto) {
        requirePositiveQuantity(dto.getQuantity());
        requirePrice(dto.getUnitPrice());
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> Cart.builder()
                .userId(userId)
                .items(new ArrayList<>())
                .total(BigDecimal.ZERO)
                .build());

        Cart.CartItem existing = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(dto.getProductId()))
                .findFirst().orElse(null);
        if (existing == null) {
            Cart.CartItem item = Cart.CartItem.builder()
                    .productId(dto.getProductId())
                    .quantity(dto.getQuantity())
                    .unitPrice(dto.getUnitPrice())
                    .build();
            cart.getItems().add(item);
        } else {
            existing.setQuantity(existing.getQuantity() + dto.getQuantity());
            // Optionally update unit price with the incoming one
            if (dto.getUnitPrice() != null) {
                existing.setUnitPrice(dto.getUnitPrice());
            }
        }
        recalcTotal(cart);
        Cart saved = cartRepository.save(cart);
        return toDTO(saved);
    }

    @Override
    public CartDTO updateItemQuantity(String userId, UpdateItemQuantityDTO dto) {
        requirePositiveQuantity(dto.getQuantity());
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found for user: " + userId));
        Cart.CartItem item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(dto.getProductId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item not found in cart: " + dto.getProductId()));
        item.setQuantity(dto.getQuantity());
        recalcTotal(cart);
        Cart saved = cartRepository.save(cart);
        return toDTO(saved);
    }

    @Override
    public CartDTO removeItem(String userId, String productId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found for user: " + userId));
        boolean removed = cart.getItems().removeIf(i -> i.getProductId().equals(productId));
        if (!removed) {
            throw new IllegalArgumentException("Item not found in cart: " + productId);
        }
        recalcTotal(cart);
        Cart saved = cartRepository.save(cart);
        return toDTO(saved);
    }

    @Override
    public CartDTO clearCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found for user: " + userId));
        cart.setItems(new ArrayList<>());
        cart.setTotal(BigDecimal.ZERO);
        Cart saved = cartRepository.save(cart);
        return toDTO(saved);
    }

    private void recalcTotal(Cart cart) {
        BigDecimal total = cart.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotal(total);
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

    private CartDTO toDTO(Cart cart) {
        List<CartItemDTO> items = cart.getItems().stream()
                .map(i -> CartItemDTO.builder()
                        .productId(i.getProductId())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .build())
                .collect(Collectors.toList());
        return CartDTO.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(items)
                .total(cart.getTotal())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
}
