package com.dic1.projettrans.productservice.repositories;

import com.dic1.projettrans.productservice.entities.SubCategory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SubCategoryRepository extends MongoRepository<SubCategory, String> {
    List<SubCategory> findByNameContainingIgnoreCase(String name);
    List<SubCategory> findByCategoryId(String categoryId);
    boolean existsByName(String name);
}
