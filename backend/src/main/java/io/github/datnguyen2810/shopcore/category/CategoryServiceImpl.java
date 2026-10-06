package io.github.datnguyen2810.shopcore.category;

import io.github.datnguyen2810.shopcore.category.dto.CategoryResponse;
import io.github.datnguyen2810.shopcore.category.dto.CreateCategoryRequest;
import io.github.datnguyen2810.shopcore.category.dto.UpdateCategoryRequest;
import io.github.datnguyen2810.shopcore.common.exception.ConflictException;
import io.github.datnguyen2810.shopcore.common.exception.ErrorCode;
import io.github.datnguyen2810.shopcore.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String normalizedSlug = request.getSlug().trim().toLowerCase(Locale.ROOT);

        if (categoryRepository.existsBySlug(normalizedSlug)) {
            throw new ConflictException(
                    ErrorCode.CATEGORY_SLUG_ALREADY_EXISTS,
                    "Category slug already exists: " + normalizedSlug
            );
        }

        Category category = Category.builder()
                                    .name(request.getName())
                                    .slug(normalizedSlug)
                                    .description(normalizeDescription(request.getDescription()))
                                    .build();

        try {
            Category savedCategory = categoryRepository.saveAndFlush(category); // để lỗi unique xảy ra bên trong try/catch vì có @Transactional nên thêm flush
            return categoryMapper.toResponse(savedCategory);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(
                    ErrorCode.CATEGORY_SLUG_ALREADY_EXISTS,
                    "Category slug already exists: " + normalizedSlug,
                    exception
            );
        }
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CATEGORY_NOT_FOUND,
                        "Category not found with id: " + id
                ));

        String normalizedSlug = request.getSlug().trim().toLowerCase(Locale.ROOT);

        if (categoryRepository.existsBySlugAndIdNot(normalizedSlug, id)) {
            throw new ConflictException(
                    ErrorCode.CATEGORY_SLUG_ALREADY_EXISTS,
                    "Category slug already exists: " + normalizedSlug
            );
        }

        category.update(request.getName().trim(), normalizedSlug, normalizeDescription(request.getDescription()));

        if (Boolean.TRUE.equals(request.getActive())) {
            category.activate();
        } else {
            category.deactivate();
        }

        try {
            categoryRepository.flush(); // chỉ cần flush() mà ko cần saveAndFlush() vì category đang được managed
            return categoryMapper.toResponse(category);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(
                    ErrorCode.CATEGORY_SLUG_ALREADY_EXISTS,
                    "Category slug already exists: " + normalizedSlug,
                    exception
            );
        }

    }

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        categoryRepository.deactivateById(id, Instant.now());
    }

    @Override
    @Transactional(readOnly = true) // Spring Boot và Hibernate hiểu rằng hàm này chỉ đọc dữ liệu từ database chứ không có bất kỳ hành động thêm, sửa, xóa nào
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAllByActiveTrueOrderByNameAsc()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID id) {
        Category category = categoryRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CATEGORY_NOT_FOUND,
                        "Active category not found with id: " + id
                ));

        return categoryMapper.toResponse(category);
    }


    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }
}
