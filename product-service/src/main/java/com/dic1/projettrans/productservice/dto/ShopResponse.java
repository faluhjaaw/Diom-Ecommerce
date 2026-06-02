package com.dic1.projettrans.productservice.dto;

import com.dic1.projettrans.productservice.entities.Shop;
import com.dic1.projettrans.productservice.entities.ShopPlan;
import com.dic1.projettrans.productservice.entities.ShopStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ShopResponse {
    private String id;
    private String slug;
    private String ownerId;
    private String name;
    private String description;
    private String logoUrl;
    private String bannerUrl;
    private String category;
    private ShopPlan plan;
    private ShopStatus status;
    private Shop.ShopStats stats;
    private Instant createdAt;
    private Instant updatedAt;

    public static ShopResponse from(Shop shop) {
        return ShopResponse.builder()
                .id(shop.getId())
                .slug(shop.getSlug())
                .ownerId(shop.getOwnerId())
                .name(shop.getName())
                .description(shop.getDescription())
                .logoUrl(shop.getLogoUrl())
                .bannerUrl(shop.getBannerUrl())
                .category(shop.getCategory())
                .plan(shop.getPlan())
                .status(shop.getStatus())
                .stats(shop.getStats())
                .createdAt(shop.getCreatedAt())
                .updatedAt(shop.getUpdatedAt())
                .build();
    }
}
