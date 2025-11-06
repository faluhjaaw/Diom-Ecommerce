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
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "sub_categories")
public class SubCategory {
    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    private String description;

    // parent Category id
    private String categoryId;

    // Liste des clés de spécifications supportées pour cette sous-catégorie
    private List<String> specificationKeys;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
