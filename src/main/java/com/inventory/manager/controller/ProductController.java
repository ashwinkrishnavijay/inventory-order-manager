package com.inventory.manager.controller;

import com.inventory.manager.dto.request.ProductRequest;
import com.inventory.manager.dto.request.StockUpdateRequest;
import com.inventory.manager.dto.response.ApiResponse;
import com.inventory.manager.dto.response.PageResponse;
import com.inventory.manager.dto.response.ProductResponse;
import com.inventory.manager.entity.ProductStatus;
import com.inventory.manager.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product & Inventory Management", description = "Endpoints for managing products, inventory levels, and product queries")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "Create product", description = "Admin only: creates a new product with stock and categories")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Update product", description = "Admin only: updates product information, price, categories")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @Operation(summary = "Update stock quantity", description = "Admin only: set new stock quantity for a product")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductResponse> updateStock(@PathVariable Long id, @Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(productService.updateStock(id, request));
    }

    @Operation(summary = "Activate or Deactivate product", description = "Admin only: set product status to ACTIVE or INACTIVE")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ProductResponse> setStatus(@PathVariable Long id, @RequestParam ProductStatus status) {
        return ResponseEntity.ok(productService.setStatus(id, status));
    }

    @Operation(summary = "Delete product", description = "Admin only: deletes a product")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.deleteProduct(id));
    }

    @Operation(summary = "Get product by ID", description = "Retrieves product details")
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @Operation(summary = "Search and filter products", description = "Search products by name, filter by category, sort by price, paginate")
    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> getProducts(
            @Parameter(description = "Filter by status (ACTIVE/INACTIVE)")
            @RequestParam(required = false) ProductStatus status,
            @Parameter(description = "Filter by category ID")
            @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Search keyword in product name")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter by maximum stock quantity")
            @RequestParam(required = false) Integer maxStock,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort property (e.g. price, name, createdAt)")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Sort sort = "desc".equalsIgnoreCase(direction) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(productService.searchProducts(status, categoryId, keyword, maxStock, pageable));
    }

    @Operation(summary = "View low-stock products", description = "Admin only: list products where stock quantity is below or equal to threshold")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/low-stock")
    public ResponseEntity<PageResponse<ProductResponse>> getLowStockProducts(
            @RequestParam(defaultValue = "5") Integer threshold,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("stockQuantity").ascending());
        return ResponseEntity.ok(productService.getLowStockProducts(threshold, pageable));
    }
}
