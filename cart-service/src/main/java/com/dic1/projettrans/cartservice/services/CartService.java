package com.dic1.projettrans.cartservice.services;

import com.dic1.projettrans.cartservice.dto.AddItemDTO;
import com.dic1.projettrans.cartservice.dto.CartDTO;
import com.dic1.projettrans.cartservice.dto.UpdateItemQuantityDTO;

import java.util.Optional;

public interface CartService {
    CartDTO getOrCreateCart(Long userId);
    Optional<CartDTO> getCart(Long userId);
    CartDTO addItem(Long userId, AddItemDTO dto);
    CartDTO updateItemQuantity(Long userId, UpdateItemQuantityDTO dto);
    CartDTO removeItem(Long userId, String productId);
    CartDTO clearCart(Long userId);
}
