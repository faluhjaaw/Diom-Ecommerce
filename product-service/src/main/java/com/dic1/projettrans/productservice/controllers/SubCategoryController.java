package com.dic1.projettrans.productservice.controllers;

import com.dic1.projettrans.productservice.dto.CreateSubCategoryDTO;
import com.dic1.projettrans.productservice.dto.SubCategoryDTO;
import com.dic1.projettrans.productservice.dto.UpdateSubCategoryDTO;
import com.dic1.projettrans.productservice.services.SubCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/subcategories")
@RequiredArgsConstructor
public class SubCategoryController {

    private final SubCategoryService subCategoryService;

    @PostMapping
    public ResponseEntity<SubCategoryDTO> create(@RequestBody CreateSubCategoryDTO dto) {
        SubCategoryDTO created = subCategoryService.create(dto);
        return ResponseEntity.created(URI.create("/api/subcategories/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubCategoryDTO> update(@PathVariable String id, @RequestBody UpdateSubCategoryDTO dto) {
        return subCategoryService.update(id, dto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        boolean deleted = subCategoryService.delete(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubCategoryDTO> getById(@PathVariable String id) {
        return subCategoryService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<SubCategoryDTO>> getAll() {
        return ResponseEntity.ok(subCategoryService.getAll());
    }

    @GetMapping("/search")
    public ResponseEntity<List<SubCategoryDTO>> searchByName(@RequestParam("query") String query) {
        return ResponseEntity.ok(subCategoryService.searchByName(query));
    }

    @GetMapping("/by-category/{categoryId}")
    public ResponseEntity<List<SubCategoryDTO>> listByCategory(@PathVariable String categoryId) {
        return ResponseEntity.ok(subCategoryService.listByCategory(categoryId));
    }
}
