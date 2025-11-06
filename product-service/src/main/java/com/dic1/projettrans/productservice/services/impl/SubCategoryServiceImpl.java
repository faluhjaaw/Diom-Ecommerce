package com.dic1.projettrans.productservice.services.impl;

import com.dic1.projettrans.productservice.dto.CreateSubCategoryDTO;
import com.dic1.projettrans.productservice.dto.SubCategoryDTO;
import com.dic1.projettrans.productservice.dto.UpdateSubCategoryDTO;
import com.dic1.projettrans.productservice.entities.SubCategory;
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

    @Override
    public SubCategoryDTO create(CreateSubCategoryDTO dto) {
        SubCategory sc = SubCategory.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .categoryId(dto.getCategoryId())
                .build();
        SubCategory saved = subCategoryRepository.save(sc);
        return toDTO(saved);
    }

    @Override
    public Optional<SubCategoryDTO> update(String id, UpdateSubCategoryDTO dto) {
        return subCategoryRepository.findById(id).map(existing -> {
            if (dto.getName() != null) existing.setName(dto.getName());
            if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
            if (dto.getCategoryId() != null) existing.setCategoryId(dto.getCategoryId());
            SubCategory saved = subCategoryRepository.save(existing);
            return toDTO(saved);
        });
    }

    @Override
    public boolean delete(String id) {
        if (!subCategoryRepository.existsById(id)) return false;
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
