package io.github.datnguyen2810.shopcore.category;

import io.github.datnguyen2810.shopcore.category.dto.CategoryResponse;
import io.github.datnguyen2810.shopcore.category.dto.CategorySearchRequest;
import io.github.datnguyen2810.shopcore.category.dto.CreateCategoryRequest;
import io.github.datnguyen2810.shopcore.category.dto.UpdateCategoryRequest;
import io.github.datnguyen2810.shopcore.common.exception.ConflictException;
import io.github.datnguyen2810.shopcore.common.exception.ErrorCode;
import io.github.datnguyen2810.shopcore.common.exception.InvalidRequestException;
import io.github.datnguyen2810.shopcore.common.exception.ResourceNotFoundException;
import io.github.datnguyen2810.shopcore.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("name", "slug", "createdAt", "updatedAt");

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
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
    @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
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
    @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
    @Transactional
    public void deleteCategory(UUID id) {
        categoryRepository.deactivateById(id, Instant.now());
    }

    @Override
    @Transactional(readOnly = true) // Spring Boot và Hibernate hiểu rằng hàm này chỉ đọc dữ liệu từ database chứ không có bất kỳ hành động thêm, sửa, xóa nào
    @PreAuthorize("#request.active == true or hasAuthority('CATALOG_MANAGE')")
    public PageResponse<CategoryResponse> searchCategories(CategorySearchRequest request) {
        String keyword = normalizeKeyword(request.getKeyword());
        Pageable pageable = PageRequest.of(
                request.getCurrentPage(), request.getPageSize(), createSort(request.getSortBy())
        );

        return PageResponse.from(categoryRepository
                .searchCategories(keyword, request.getActive(), pageable)
                .map(categoryMapper::toResponse));
    }

    private Sort createSort(String sortBy) {
        List<Sort.Order> orders = new ArrayList<>();
        if (sortBy == null) {
            orders.add(Sort.Order.asc("name"));
        } else {
            // name:asc,price:desc
            for (String expression : sortBy.split(",", -1)) {
                String[] parts = expression.split(":", -1);
                if (parts.length != 2 || !ALLOWED_SORT_FIELDS.contains(parts[0])) {
                    throw invalidSort(sortBy);
                }

                Sort.Direction direction = switch (parts[1].toLowerCase(Locale.ROOT)) {
                    case "asc" -> Sort.Direction.ASC;
                    case "desc" -> Sort.Direction.DESC;
                    default -> throw invalidSort(sortBy);
                };
                orders.add(new Sort.Order(direction, parts[0]));
            }
        }
        orders.add(Sort.Order.asc("id"));
        return Sort.by(orders);
    }

    private InvalidRequestException invalidSort(String sortBy) {
        return new InvalidRequestException(
                ErrorCode.INVALID_SORT_PARAMETER,
                "Invalid sort parameter: " + sortBy
        );
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "";
        }
        return keyword.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
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
