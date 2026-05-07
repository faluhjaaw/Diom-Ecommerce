package com.dic1.projettrans.productservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventProducer {

    public static final String PRODUCT_VIEWED_TOPIC  = "user.product_viewed";
    public static final String PRODUCT_CATALOG_TOPIC = "product.catalog";

    // KafkaTemplate<String, Object> : JsonSerializer gère tous les types
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishProductViewed(ProductViewedEvent event) {
        kafkaTemplate.send(PRODUCT_VIEWED_TOPIC, event.getProductId(), event);
        log.info("ProductViewed publié → productId={}, user={}", event.getProductId(), event.getUserEmail());
    }

    public void publishProductCreated(ProductCatalogEvent event) {
        kafkaTemplate.send(PRODUCT_CATALOG_TOPIC, event.getId(), event);
        log.info("ProductCreated publié → productId={}", event.getId());
    }

    public void publishProductUpdated(ProductCatalogEvent event) {
        kafkaTemplate.send(PRODUCT_CATALOG_TOPIC, event.getId(), event);
        log.info("ProductUpdated publié → productId={}", event.getId());
    }

    public void publishProductDeleted(String productId) {
        ProductCatalogEvent event = ProductCatalogEvent.builder()
                .eventType("deleted")
                .id(productId)
                .build();
        kafkaTemplate.send(PRODUCT_CATALOG_TOPIC, productId, event);
        log.info("ProductDeleted publié → productId={}", productId);
    }
}
