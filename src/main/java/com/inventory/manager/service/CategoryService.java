package com.inventory.manager.service;

import com.inventory.manager.dto.request.CategoryRequest;
import com.inventory.manager.dto.response.ApiResponse;
import com.inventory.manager.dto.response.CategoryResponse;
import com.inventory.manager.entity.Category;
import com.inventory.manager.exception.BadRequestException;
import com.inventory.manager.exception.ResourceNotFoundException;
import com.inventory.manager.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String name = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("Category already exists: " + name);
        }

        Category category = new Category(name, request.getDescription() != null ? request.getDescription().trim() : null);
        Category saved = categoryRepository.save(category);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = getCategoryEntity(id);
        return mapToResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = getCategoryEntity(id);
        String newName = request.getName().trim();

        if (!category.getName().equalsIgnoreCase(newName) && categoryRepository.existsByNameIgnoreCase(newName)) {
            throw new BadRequestException("Category name already in use: " + newName);
        }

        category.setName(newName);
        category.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);

        Category updated = categoryRepository.save(category);
        return mapToResponse(updated);
    }

    @Transactional
    public ApiResponse deleteCategory(Long id) {
        Category category = getCategoryEntity(id);
        categoryRepository.delete(category);
        return new ApiResponse(true, "Category '" + category.getName() + "' deleted successfully");
    }

    public Category getCategoryEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
    }

    public CategoryResponse mapToResponse(Category category) {
        int count = category.getProducts() != null ? category.getProducts().size() : 0;
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                count,
                category.getCreatedAt()
        );
    }
}
