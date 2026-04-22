package com.dic1.projettrans.cartservice.kafka;

import com.dic1.projettrans.cartservice.services.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final CartService cartService;

    @KafkaListener(topics = "order.created", groupId = "cart-service-group")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Événement OrderCreated reçu → orderId={}, userId={}", event.getOrderId(), event.getUserId());
        try {
            cartService.clearCart(event.getUserId());
            log.info("Panier vidé pour l'utilisateur {}", event.getUserId());
        } catch (Exception e) {
            log.error("Échec du vidage du panier pour l'utilisateur {} : {}", event.getUserId(), e.getMessage());
        }
    }
}
