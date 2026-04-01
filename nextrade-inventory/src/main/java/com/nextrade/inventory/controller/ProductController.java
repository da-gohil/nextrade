package com.nextrade.inventory.controller;

import com.nextrade.common.dto.ApiResponse;
import com.nextrade.common.dto.PageResponse;
import com.nextrade.inventory.dto.*;
import com.nextrade.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.status(201).body(ApiResponse.created(inventoryService.createProduct(request, userId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductDto>>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(sort).descending());
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getProducts(search, categoryId, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getProduct(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody CreateProductRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.updateProduct(id, request, userId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        inventoryService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted", null));
    }

    @GetMapping("/{id}/stock")
    public ResponseEntity<ApiResponse<Integer>> getStock(@PathVariable Long id) {
        ProductDto product = inventoryService.getProduct(id);
        return ResponseEntity.ok(ApiResponse.success(product.getAvailableQuantity()));
    }

    @PutMapping("/{id}/stock")
    public ResponseEntity<ApiResponse<ProductDto>> adjustStock(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStockRequest request) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.adjustStock(id, request)));
    }
}
