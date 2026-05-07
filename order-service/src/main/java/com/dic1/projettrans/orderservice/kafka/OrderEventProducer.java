package com.dic1.projettrans.orderservice.kafka;

import com.dic1.projettrans.orderservice.events.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    public static final String ORDER_CREATED_TOPIC = "user.order_completed";

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        kafkaTemplate.send(ORDER_CREATED_TOPIC, event.getOrderId(), event);
        log.info("Événement OrderCreated publié → orderId={}, userId={}", event.getOrderId(), event.getUserId());
    }
}
