package com.dic1.projettrans.productservice.services;

import com.dic1.projettrans.productservice.dto.CategoryDTO;
import com.dic1.projettrans.productservice.dto.CreateCategoryDTO;
import com.dic1.projettrans.productservice.dto.UpdateCategoryDTO;

import java.util.List;
import java.util.Optional;

public interface CategoryService {
    CategoryDTO create(CreateCategoryDTO dto);
    Optional<CategoryDTO> update(String id, UpdateCategoryDTO dto);
    boolean delete(String id);
    Optional<CategoryDTO> getById(String id);
    List<CategoryDTO> getAll();
    List<CategoryDTO> getChildren(String parentId);
}
