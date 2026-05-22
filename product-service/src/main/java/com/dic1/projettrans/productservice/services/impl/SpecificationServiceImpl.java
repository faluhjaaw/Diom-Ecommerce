package com.dic1.projettrans.productservice.services.impl;

import com.dic1.projettrans.productservice.dto.SpecificationDTO.SpecificationFilter;
import com.dic1.projettrans.productservice.dto.SpecificationDTO.SpecificationFilterRequest;
import com.dic1.projettrans.productservice.entities.CategorySpecification;
import com.dic1.projettrans.productservice.entities.Product;
import com.dic1.projettrans.productservice.entities.Product.SpecificationDefinition;
import com.dic1.projettrans.productservice.entities.Product.SpecificationValue;
import com.dic1.projettrans.productservice.repositories.CategorySpecificationRepository;
import com.dic1.projettrans.productservice.repositories.SubCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import com.dic1.projettrans.productservice.services.SpecificationService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpecificationServiceImpl implements SpecificationService {

    private final CategorySpecificationRepository categorySpecificationRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public List<Product> searchProductsBySpecifications(SpecificationFilterRequest filterRequest) {
        Query query = new Query();

        // Filter by subCategoryId (direct)
        if (filterRequest.getSubCategoryId() != null) {
            query.addCriteria(Criteria.where("subCategoryId").is(filterRequest.getSubCategoryId()));
        }
        // Filter by categoryId: find all subCategoryIds belonging to this category
        else if (filterRequest.getCategoryId() != null) {
            List<String> subCategoryIds = subCategoryRepository
                    .findByCategoryId(filterRequest.getCategoryId())
                    .stream()
                    .map(sc -> sc.getId())
                    .collect(Collectors.toList());
            if (subCategoryIds.isEmpty()) {
                return List.of();
            }
            query.addCriteria(Criteria.where("subCategoryId").in(subCategoryIds));
        }

        if (filterRequest.getFilters() != null) {
            for (SpecificationFilter filter : filterRequest.getFilters()) {
                String key = "specifications." + filter.getName() + ".value";
                Object val = filter.getValue();

                switch (filter.getOperation()) {
                    case EQUALS:
                        query.addCriteria(Criteria.where(key).is(val));
                        break;
                    case GREATER_THAN:
                        query.addCriteria(Criteria.where(key).gt(val));
                        break;
                    case LESS_THAN:
                        query.addCriteria(Criteria.where(key).lt(val));
                        break;
                    case CONTAINS:
                        query.addCriteria(Criteria.where(key).regex(val.toString(), "i"));
                        break;
                    case IN:
                        if (val instanceof java.util.List) {
                            query.addCriteria(Criteria.where(key).in((java.util.List<?>) val));
                        } else {
                            query.addCriteria(Criteria.where(key).is(val));
                        }
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
        // Upsert: update existing definition if one exists for this subCategory
        Optional<CategorySpecification> existing = categorySpecificationRepository.findFirstBySubCategoryId(subCategoryId);
        CategorySpecification categorySpec = existing.orElseGet(() ->
                CategorySpecification.builder().subCategoryId(subCategoryId).build()
        );
        categorySpec.setSpecifications(specifications);
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
                String specValue = product.getSpecifications().get(specDef.getName());
                if (specValue == null || specValue.isBlank()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean validateSpecificationValue(Object value, SpecificationValue.SpecificationType type, SpecificationDefinition definition) {
        if (type != definition.getType()) {
            return false;
        }
        switch (type) {
            case ENUM:
                return definition.getAllowedValues().contains(value);
            case NUMBER:
                return value instanceof Number;
            case BOOLEAN:
                return value instanceof Boolean;
            case TEXT:
                return value instanceof String;
            default:
                return false;
        }
    }
}
