package com.dic1.projettrans.productservice.services;


import com.dic1.projettrans.productservice.dto.SpecificationDTO;
import com.dic1.projettrans.productservice.entities.CategorySpecification;
import com.dic1.projettrans.productservice.entities.Product;

import java.util.List;

public interface SpecificationService {
    List<Product> searchProductsBySpecifications(SpecificationDTO.SpecificationFilterRequest filterRequest);
    CategorySpecification saveSpecificationDefinition(String subCategoryId, List<Product.SpecificationDefinition> specifications);
    boolean validateProductSpecifications(Product product, CategorySpecification categorySpec);
    boolean validateSpecificationValue(String value, Product.SpecificationValue.SpecificationType type, Product.SpecificationDefinition definition);
}