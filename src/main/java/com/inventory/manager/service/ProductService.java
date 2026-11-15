package com.inventory.manager.service;

import com.inventory.manager.dto.request.ProductRequest;
import com.inventory.manager.dto.request.StockUpdateRequest;
import com.inventory.manager.dto.response.ApiResponse;
import com.inventory.manager.dto.response.CategoryResponse;
import com.inventory.manager.dto.response.PageResponse;
import com.inventory.manager.dto.response.ProductResponse;
import com.inventory.manager.entity.Category;
import com.inventory.manager.entity.Product;
import com.inventory.manager.entity.ProductStatus;
import com.inventory.manager.exception.ResourceNotFoundException;
import com.inventory.manager.repository.CategoryRepository;
import com.inventory.manager.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          CategoryService categoryService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.categoryService = categoryService;
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product(
                request.getName().trim(),
                request.getDescription() != null ? request.getDescription().trim() : null,
                request.getPrice(),
                request.getStockQuantity(),
                request.getStatus() != null ? request.getStatus() : ProductStatus.ACTIVE
        );

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));
            product.setCategories(categories);
        }

        Product saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = getProductEntity(id);

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }

        if (request.getCategoryIds() != null) {
            Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));
            product.setCategories(categories);
        }

        Product updated = productRepository.save(product);
        return mapToResponse(updated);
    }

    @Transactional
    public ProductResponse updateStock(Long id, StockUpdateRequest request) {
        Product product = getProductEntity(id);
        product.setStockQuantity(request.getStockQuantity());
        Product updated = productRepository.save(product);
        return mapToResponse(updated);
    }

    @Transactional
    public ProductResponse setStatus(Long id, ProductStatus status) {
        Product product = getProductEntity(id);
        product.setStatus(status);
        Product updated = productRepository.save(product);
        return mapToResponse(updated);
    }

    @Transactional
    public ApiResponse deleteProduct(Long id) {
        Product product = getProductEntity(id);
        productRepository.delete(product);
        return new ApiResponse(true, "Product '" + product.getName() + "' deleted successfully");
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = getProductEntity(id);
        return mapToResponse(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(ProductStatus status, Long categoryId, String keyword, Integer maxStock, Pageable pageable) {
        Page<Product> page = productRepository.searchProducts(status, categoryId, keyword, maxStock, pageable);
        return PageResponse.from(page.map(this::mapToResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getLowStockProducts(Integer threshold, Pageable pageable) {
        int max = (threshold != null) ? threshold : 5;
        Page<Product> page = productRepository.findByStockQuantityLessThanEqual(max, pageable);
        return PageResponse.from(page.map(this::mapToResponse));
    }

    public Product getProductEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
    }

    public ProductResponse mapToResponse(Product product) {
        Set<CategoryResponse> categoryResponses = product.getCategories().stream()
                .map(categoryService::mapToResponse)
                .collect(Collectors.toSet());

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getStatus(),
                categoryResponses,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
