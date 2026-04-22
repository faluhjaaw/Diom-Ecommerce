package com.dic1.projettrans.productservice.services.impl;

import com.dic1.projettrans.productservice.dto.CategoryDTO;
import com.dic1.projettrans.productservice.dto.CreateCategoryDTO;
import com.dic1.projettrans.productservice.dto.UpdateCategoryDTO;
import com.dic1.projettrans.productservice.entities.Category;
import com.dic1.projettrans.productservice.repositories.CategoryRepository;
import com.dic1.projettrans.productservice.repositories.CategorySpecificationRepository;
import com.dic1.projettrans.productservice.repositories.SubCategoryRepository;
import com.dic1.projettrans.productservice.services.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final CategorySpecificationRepository categorySpecificationRepository;

    @Override
    public CategoryDTO create(CreateCategoryDTO dto) {
        if (dto.getName() != null && categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("Une catégorie avec ce nom existe déjà : " + dto.getName());
        }
        Category category = Category.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .build();
        return toDTO(categoryRepository.save(category));
    }

    @Override
    public Optional<CategoryDTO> update(String id, UpdateCategoryDTO dto) {
        return categoryRepository.findById(id).map(existing -> {
            if (dto.getName() != null) existing.setName(dto.getName());
            if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
            return toDTO(categoryRepository.save(existing));
        });
    }

    @Override
    public boolean delete(String id) {
        if (!categoryRepository.existsById(id)) return false;
        // Cascade : supprimer les specs des sous-catégories puis les sous-catégories
        List<String> subCategoryIds = subCategoryRepository.findByCategoryId(id)
                .stream().map(sc -> sc.getId()).collect(Collectors.toList());
        if (!subCategoryIds.isEmpty()) {
            categorySpecificationRepository.deleteBySubCategoryIdIn(subCategoryIds);
            subCategoryRepository.deleteByCategoryId(id);
        }
        categoryRepository.deleteById(id);
        return true;
    }

    @Override
    public Optional<CategoryDTO> getById(String id) {
        return categoryRepository.findById(id).map(this::toDTO);
    }

    @Override
    public List<CategoryDTO> getAll() {
        return categoryRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<CategoryDTO> getChildren(String categoryId) {
        // retourne les sous-catégories comme enfants de la catégorie
        return subCategoryRepository.findByCategoryId(categoryId).stream()
                .map(sc -> CategoryDTO.builder()
                        .id(sc.getId())
                        .name(sc.getName())
                        .description(sc.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    private CategoryDTO toDTO(Category category) {
        if (category == null) return null;
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}
