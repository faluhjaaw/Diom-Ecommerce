package com.dic1.projettrans.cartservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartEventProducer {

    public static final String CART_ITEM_ADDED_TOPIC = "user.cart_added";

    private final KafkaTemplate<String, CartItemAddedEvent> kafkaTemplate;

    public void publishCartItemAdded(CartItemAddedEvent event) {
        kafkaTemplate.send(CART_ITEM_ADDED_TOPIC, event.getUserId().toString(), event);
        log.info("Événement CartItemAdded publié → productId={}, userId={}", event.getProductId(), event.getUserId());
    }
}
