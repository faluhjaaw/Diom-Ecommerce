package com.dic1.projettrans.productservice.kafka;

import com.dic1.projettrans.productservice.entities.ListingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Événement publié sur product.created / product.updated / product.deleted.
 * Contient tous les champs nécessaires à la génération de l'embedding côté recommendation-service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCatalogEvent {
    /** "created" | "updated" | "deleted" */
    private String eventType;
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String subCategoryId;
    private String brand;
    private List<String> tags;
    private List<String> imageUrls;
    private String slug;
    private Double rating;
    private ListingStatus status;
}
