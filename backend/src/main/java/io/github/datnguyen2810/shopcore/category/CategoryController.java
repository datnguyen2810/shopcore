package io.github.datnguyen2810.shopcore.category;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.datnguyen2810.shopcore.category.dto.CategoryResponse;
import io.github.datnguyen2810.shopcore.category.dto.CategorySearchRequest;
import io.github.datnguyen2810.shopcore.common.response.ApiResponse;
import io.github.datnguyen2810.shopcore.common.response.PageResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getCategories(
            @Valid @ModelAttribute CategorySearchRequest request
    ) {
        PageResponse<CategoryResponse> categories = categoryService.searchCategories(request);

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Categories retrieved successfully", categories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable UUID id) {
        CategoryResponse category = categoryService.getCategoryById(id);

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Category retrieved successfully", category));
    }



}
