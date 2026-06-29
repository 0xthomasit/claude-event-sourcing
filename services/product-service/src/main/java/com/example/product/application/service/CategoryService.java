package com.example.product.application.service;

import com.example.product.application.dto.CategoryResponse;
import com.example.product.application.dto.CreateCategoryRequest;
import com.example.product.domain.model.Category;
import com.example.product.domain.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        if (categoryRepository.existsBySlug(request.getSlug()))
            throw new IllegalArgumentException("Slug already exists: " + request.getSlug());

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Parent category not found: " + request.getParentId()));
        }

        Category saved = categoryRepository.save(Category.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .parent(parent)
                .build());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findRoots() {
        return categoryRepository.findByParentIsNull().stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findChildren(UUID parentId) {
        return categoryRepository.findByParentId(parentId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(UUID id) {
        return categoryRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Category not found: " + id));
    }

    private CategoryResponse toResponse(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .slug(c.getSlug())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .parentName(c.getParent() != null ? c.getParent().getName() : null)
                .build();
    }
}