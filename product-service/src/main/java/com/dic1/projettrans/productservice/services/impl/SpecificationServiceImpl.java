package com.dic1.projettrans.productservice.services.impl;

import com.dic1.projettrans.productservice.dto.SpecificationDTO.SpecificationFilter;
import com.dic1.projettrans.productservice.dto.SpecificationDTO.SpecificationFilterRequest;
import com.dic1.projettrans.productservice.entities.CategorySpecification;
import com.dic1.projettrans.productservice.entities.Product;
import com.dic1.projettrans.productservice.entities.Product.SpecificationDefinition;
import com.dic1.projettrans.productservice.entities.Product.SpecificationValue;
import com.dic1.projettrans.productservice.repositories.CategorySpecificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import com.dic1.projettrans.productservice.services.SpecificationService;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpecificationServiceImpl implements SpecificationService {

    private final CategorySpecificationRepository categorySpecificationRepository;

    private MongoTemplate mongoTemplate;

    @Override
    public List<Product> searchProductsBySpecifications(SpecificationFilterRequest filterRequest) {
        Query query = new Query();
        
        if (filterRequest.getSubCategoryId() != null) {
            query.addCriteria(Criteria.where("subCategoryId").is(filterRequest.getSubCategoryId()));
        }
        
        if (filterRequest.getFilters() != null) {
            for (SpecificationFilter filter : filterRequest.getFilters()) {
                String key = "specifications." + filter.getName() + ".value";
                
                switch (filter.getOperation()) {
                    case EQUALS:
                        query.addCriteria(Criteria.where(key).is(filter.getValue()));
                        break;
                    case GREATER_THAN:
                        query.addCriteria(Criteria.where(key).gt(filter.getValue()));
                        break;
                    case LESS_THAN:
                        query.addCriteria(Criteria.where(key).lt(filter.getValue()));
                        break;
                    case CONTAINS:
                        query.addCriteria(Criteria.where(key).regex(filter.getValue(), "i"));
                        break;
                    case IN:
                        String[] values = filter.getValue().split(",");
                        query.addCriteria(Criteria.where(key).in((Object[]) values));
                        break;
                }
            }
        }
        
        return mongoTemplate.find(query, Product.class);
    }

    @Override
    public List<Product.SpecificationDefinition> getSpecificationsBySubCategoryId(String subCategoryId) {
        List<CategorySpecification> list = categorySpecificationRepository.findBySubCategoryId(subCategoryId);

        return list.stream()
                .filter(Objects::nonNull)
                .flatMap(cs -> cs.getSpecifications().stream())
                .collect(Collectors.toList());
    }


    @Override
    public CategorySpecification saveSpecificationDefinition(String subCategoryId, List<SpecificationDefinition> specifications) {
        CategorySpecification categorySpec = CategorySpecification.builder()
            .subCategoryId(subCategoryId)
            .specifications(specifications)
            .build();
        return categorySpecificationRepository.save(categorySpec);
    }

    @Override
    public boolean validateProductSpecifications(Product product, CategorySpecification categorySpec) {
        if (product.getSpecifications() == null) {
            return false;
        }

        for (SpecificationDefinition specDef : categorySpec.getSpecifications()) {
            if (specDef.isRequired() && !product.getSpecifications().containsKey(specDef.getName())) {
                return false;
            }

            if (product.getSpecifications().containsKey(specDef.getName())) {
                SpecificationValue specValue = product.getSpecifications().get(specDef.getName());
                if (!validateSpecificationValue(specValue.getValue(), specValue.getType(), specDef)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean validateSpecificationValue(String value, SpecificationValue.SpecificationType type, SpecificationDefinition definition) {
        if (type != definition.getType()) {
            return false;
        }

        switch (type) {
            case ENUM:
                return definition.getAllowedValues().contains(value);
            case NUMBER:
                try {
                    Double.parseDouble(value);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            case BOOLEAN:
                return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
            case TEXT:
                return true;
            default:
                return false;
        }
    }
}