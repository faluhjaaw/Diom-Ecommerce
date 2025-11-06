package com.dic1.projettrans.productservice.services;

import com.dic1.projettrans.productservice.dto.CreateSubCategoryDTO;
import com.dic1.projettrans.productservice.dto.SubCategoryDTO;
import com.dic1.projettrans.productservice.dto.UpdateSubCategoryDTO;

import java.util.List;
import java.util.Optional;

public interface SubCategoryService {
    SubCategoryDTO create(CreateSubCategoryDTO dto);
    Optional<SubCategoryDTO> update(String id, UpdateSubCategoryDTO dto);
    boolean delete(String id);
    Optional<SubCategoryDTO> getById(String id);
    List<SubCategoryDTO> getAll();
    List<SubCategoryDTO> searchByName(String query);
    List<SubCategoryDTO> listByCategory(String categoryId);
}
