package com.dic1.projettrans.orderservice.services;

import com.dic1.projettrans.orderservice.feign.CartServiceRestClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartAsyncService {

    private final CartServiceRestClient cartClient;

    @Async("orderTaskExecutor")
    public void clearCart(Long userId) {
        try {
            cartClient.clearCart(userId);
            log.info("Panier vidé pour l'utilisateur {}", userId);
        } catch (Exception e) {
            log.warn("Échec du vidage du panier pour l'utilisateur {} : {}", userId, e.getMessage());
        }
    }
}
