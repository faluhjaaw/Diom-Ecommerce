package com.dic1.projettrans.productservice.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "shops")
public class Shop {

    @Id
    private String id;

    @Indexed(unique = true)
    private String slug;

    @Indexed
    private String ownerId; // email du propriétaire (issu du JWT)

    private String name;
    private String description;
    private String logoUrl;
    private String bannerUrl;
    private String category;

    @Builder.Default
    private ShopPlan plan = ShopPlan.FREE;

    @Builder.Default
    private ShopStatus status = ShopStatus.PENDING;

    @Builder.Default
    private ShopStats stats = new ShopStats();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShopStats {
        @Builder.Default private int totalProducts = 0;
        @Builder.Default private int totalSales = 0;
        @Builder.Default private double totalRevenue = 0.0;
        @Builder.Default private double rating = 0.0;
        @Builder.Default private int reviewCount = 0;
    }
}
