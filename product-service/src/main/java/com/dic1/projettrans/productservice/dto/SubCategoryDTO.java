package com.dic1.projettrans.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubCategoryDTO {
    private String id;
    private String name;
    private String description;
    private String categoryId;
    private Instant createdAt;
    private Instant updatedAt;
}
