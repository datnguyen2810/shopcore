package io.github.datnguyen2810.shopcore.category;

import io.github.datnguyen2810.shopcore.category.dto.CategoryResponse;
import io.github.datnguyen2810.shopcore.category.dto.CategorySearchRequest;
import io.github.datnguyen2810.shopcore.category.dto.CreateCategoryRequest;
import io.github.datnguyen2810.shopcore.category.dto.UpdateCategoryRequest;
import io.github.datnguyen2810.shopcore.common.response.PageResponse;

import java.util.UUID;

public interface CategoryService {

    CategoryResponse createCategory(CreateCategoryRequest request);

    CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request);

    void deleteCategory(UUID id);

    PageResponse<CategoryResponse> searchCategories(CategorySearchRequest request);

    CategoryResponse getCategoryById(UUID id);
}
