package io.github.datnguyen2810.shopcore.category;

import io.github.datnguyen2810.shopcore.category.dto.CategoryResponse;
import io.github.datnguyen2810.shopcore.category.dto.CreateCategoryRequest;
import io.github.datnguyen2810.shopcore.category.dto.UpdateCategoryRequest;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    CategoryResponse createCategory(CreateCategoryRequest request);

    CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request);

    void deleteCategory(UUID id);

    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryById(UUID id);
}
