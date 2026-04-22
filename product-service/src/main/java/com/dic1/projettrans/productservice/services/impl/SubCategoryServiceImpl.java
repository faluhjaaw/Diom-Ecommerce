package com.dic1.projettrans.productservice.services.impl;

import com.dic1.projettrans.productservice.dto.CreateSubCategoryDTO;
import com.dic1.projettrans.productservice.dto.SubCategoryDTO;
import com.dic1.projettrans.productservice.dto.UpdateSubCategoryDTO;
import com.dic1.projettrans.productservice.entities.SubCategory;
import com.dic1.projettrans.productservice.repositories.CategoryRepository;
import com.dic1.projettrans.productservice.repositories.CategorySpecificationRepository;
import com.dic1.projettrans.productservice.repositories.SubCategoryRepository;
import com.dic1.projettrans.productservice.services.SubCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubCategoryServiceImpl implements SubCategoryService {

    private final SubCategoryRepository subCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final CategorySpecificationRepository categorySpecificationRepository;

    @Override
    public SubCategoryDTO create(CreateSubCategoryDTO dto) {
        if (dto.getCategoryId() == null || !categoryRepository.existsById(dto.getCategoryId())) {
            throw new IllegalArgumentException("Catégorie inexistante : " + dto.getCategoryId());
        }
        if (dto.getName() != null && subCategoryRepository.existsByNameAndCategoryId(dto.getName(), dto.getCategoryId())) {
            throw new IllegalArgumentException("Une sous-catégorie avec ce nom existe déjà dans cette catégorie : " + dto.getName());
        }
        SubCategory sc = SubCategory.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .categoryId(dto.getCategoryId())
                .build();
        return toDTO(subCategoryRepository.save(sc));
    }

    @Override
    public Optional<SubCategoryDTO> update(String id, UpdateSubCategoryDTO dto) {
        return subCategoryRepository.findById(id).map(existing -> {
            if (dto.getCategoryId() != null) {
                if (!categoryRepository.existsById(dto.getCategoryId())) {
                    throw new IllegalArgumentException("Catégorie inexistante : " + dto.getCategoryId());
                }
                existing.setCategoryId(dto.getCategoryId());
            }
            if (dto.getName() != null) existing.setName(dto.getName());
            if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
            return toDTO(subCategoryRepository.save(existing));
        });
    }

    @Override
    public boolean delete(String id) {
        if (!subCategoryRepository.existsById(id)) return false;
        categorySpecificationRepository.deleteBySubCategoryId(id);
        subCategoryRepository.deleteById(id);
        return true;
    }

    @Override
    public Optional<SubCategoryDTO> getById(String id) {
        return subCategoryRepository.findById(id).map(this::toDTO);
    }

    @Override
    public List<SubCategoryDTO> getAll() {
        return subCategoryRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<SubCategoryDTO> searchByName(String query) {
        return subCategoryRepository.findByNameContainingIgnoreCase(query)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<SubCategoryDTO> listByCategory(String categoryId) {
        return subCategoryRepository.findByCategoryId(categoryId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    private SubCategoryDTO toDTO(SubCategory sc) {
        if (sc == null) return null;
        return SubCategoryDTO.builder()
                .id(sc.getId())
                .name(sc.getName())
                .description(sc.getDescription())
                .categoryId(sc.getCategoryId())
                .createdAt(sc.getCreatedAt())
                .updatedAt(sc.getUpdatedAt())
                .build();
    }
}
