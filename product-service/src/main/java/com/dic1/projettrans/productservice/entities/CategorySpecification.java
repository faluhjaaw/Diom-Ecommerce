package com.dic1.projettrans.productservice.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "category_specifications")
public class CategorySpecification {
    @Id
    private String id;
    
    private String subCategoryId;
    
    private List<Product.SpecificationDefinition> specifications;
}