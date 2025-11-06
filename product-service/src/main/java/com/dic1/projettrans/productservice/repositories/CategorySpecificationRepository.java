package com.dic1.projettrans.productservice.repositories;

import com.dic1.projettrans.productservice.entities.CategorySpecification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategorySpecificationRepository extends MongoRepository<CategorySpecification, String> {
    Optional<CategorySpecification> findBySubCategoryId(String subCategoryId);
}