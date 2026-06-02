package com.dic1.projettrans.productservice.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder.Default;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ── Nested types used by CategorySpecification schema (not by product specs field) ──

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "products")
public class Product {
    @Id
    private String id;

    @Indexed
    private String name;

    private String description;

    private BigDecimal price;

    private Integer stock;

    private String subCategoryId;

    private String brand;

    private List<String> imageUrls;

    @Indexed
    private List<String> tags;

    private ProductCondition condition;

    /** Calculated from avis — never set by client directly */
    private Double rating;

    /** Simple key/value specifications (e.g. "RAM" -> "8Go") */
    private Map<String, String> specifications = new HashMap<>();

    @Indexed(unique = true)
    private String slug;

    // Marketplace fields
    private String location;
    private boolean negotiable;
    private String contactPhone;

    @Indexed
    private String sellerEmail;

    @Indexed
    private ListingStatus status = ListingStatus.ACTIVE;

    /** null = annonce C2C classique, non-null = produit d'une boutique */
    @Indexed
    private String shopId;

    /** CUSTOMER par défaut (rétrocompatible avec données existantes) */
    @Default
    private SellerType sellerType = SellerType.CUSTOMER;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // ── Schema/validation inner classes (used by CategorySpecification, not stored here) ──

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SpecificationValue {
        private Object value;
        private SpecificationType type;

        public enum SpecificationType {
            TEXT, NUMBER, BOOLEAN, ENUM
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SpecificationDefinition {
        private String name;
        private SpecificationValue.SpecificationType type;
        private String description;
        private boolean required;
        private List<String> allowedValues;
    }
}
