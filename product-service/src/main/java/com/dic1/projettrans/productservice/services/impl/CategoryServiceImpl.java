package com.dic1.projettrans.productservice.services.impl;

import com.dic1.projettrans.productservice.dto.CategoryDTO;
import com.dic1.projettrans.productservice.dto.CreateCategoryDTO;
import com.dic1.projettrans.productservice.dto.UpdateCategoryDTO;
import com.dic1.projettrans.productservice.entities.Category;
import com.dic1.projettrans.productservice.repositories.CategoryRepository;
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

    @Override
    public CategoryDTO create(CreateCategoryDTO dto) {
        // Ensure unique name (basic check)
        if (dto.getName() != null && categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("Category name already exists: " + dto.getName());
        }
        Category category = Category.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .parentId(dto.getParentId())
                .build();
        Category saved = categoryRepository.save(category);
        return toDTO(saved);
    }

    @Override
    public Optional<CategoryDTO> update(String id, UpdateCategoryDTO dto) {
        return categoryRepository.findById(id).map(existing -> {
            if (dto.getName() != null) existing.setName(dto.getName());
            if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
            if (dto.getParentId() != null) existing.setParentId(dto.getParentId());
            Category saved = categoryRepository.save(existing);
            return toDTO(saved);
        });
    }

    @Override
    public boolean delete(String id) {
        if (!categoryRepository.existsById(id)) return false;
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
    public List<CategoryDTO> getChildren(String parentId) {
        return categoryRepository.findByParentId(parentId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    private CategoryDTO toDTO(Category category) {
        if (category == null) return null;
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .image(category.getImage())
                .description(category.getDescription())
                .parentId(category.getParentId())
                .build();
    }
}
