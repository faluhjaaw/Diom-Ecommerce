package com.dic1.projettrans.productservice.repositories;

import com.dic1.projettrans.productservice.entities.Category;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CategoryRepository extends MongoRepository<Category, String> {
    boolean existsByNameIgnoreCase(String name);
    List<Category> findByParentId(String parentId);
}
