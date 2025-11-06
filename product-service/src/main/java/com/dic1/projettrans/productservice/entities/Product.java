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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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

    // reference to SubCategory id
    private String subCategoryId;

    // reference to vendor (seller) user id
    @Indexed
    private String vendorId;

    private String brand;

    private List<String> imageUrls;

    // tags associated with the product (e.g., "smartphone", "5g")
    @Indexed
    private List<String> tags;

    // Etat/condition du produit: NEW(neuf), USED(d'occasion), REFURBISHED(reconditionné)
    private ProductCondition condition;

    // Note/évaluation moyenne (0.0 - 5.0)
    private Double rating;

    // Specifications dynamiques du produit (clé/valeur)
    @Indexed
    private Map<String, SpecificationValue> specifications;

    // URL-friendly unique slug derived from the name
    @Indexed(unique = true)
    private String slug;

    // Classe interne pour les valeurs de spécification
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SpecificationValue {
        private String value;
        private SpecificationType type;

        public enum SpecificationType {
            TEXT,
            NUMBER,
            BOOLEAN,
            ENUM
        }
    }

    // Classe interne pour la définition des spécifications
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

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
