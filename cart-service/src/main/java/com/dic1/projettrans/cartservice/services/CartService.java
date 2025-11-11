package com.dic1.projettrans.cartservice.services;

import com.dic1.projettrans.cartservice.dto.AddItemDTO;
import com.dic1.projettrans.cartservice.dto.CartDTO;
import com.dic1.projettrans.cartservice.dto.UpdateItemQuantityDTO;

import java.util.Optional;

public interface CartService {
    CartDTO getOrCreateCart(String userId);
    Optional<CartDTO> getCart(String userId);
    CartDTO addItem(String userId, AddItemDTO dto);
    CartDTO updateItemQuantity(String userId, UpdateItemQuantityDTO dto);
    CartDTO removeItem(String userId, String productId);
    CartDTO clearCart(String userId);
}
