package io.github.datnguyen2810.shopcore.product;

import io.github.datnguyen2810.shopcore.category.Category;
import io.github.datnguyen2810.shopcore.category.CategoryRepository;
import io.github.datnguyen2810.shopcore.common.exception.ConflictException;
import io.github.datnguyen2810.shopcore.common.exception.ErrorCode;
import io.github.datnguyen2810.shopcore.common.exception.InvalidRequestException;
import io.github.datnguyen2810.shopcore.common.exception.ResourceNotFoundException;
import io.github.datnguyen2810.shopcore.common.response.PageResponse;
import io.github.datnguyen2810.shopcore.product.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService{
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.of(
            "name", "name",
            "price", "price",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt"
    );

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        String normalizedSku = request.getSku().trim().toUpperCase(Locale.ROOT);
        String normalizedSlug = request.getSlug().trim().toLowerCase(Locale.ROOT);

        if(productRepository.existsBySku(normalizedSku)){
            throw new ConflictException(
                    ErrorCode.PRODUCT_SKU_ALREADY_EXISTS,
                    "Product sku already exists: " + normalizedSku);
        }

        if(productRepository.existsBySlug(normalizedSlug)){
            throw new ConflictException(
                    ErrorCode.PRODUCT_SLUG_ALREADY_EXISTS,
                    "Product slug already exists: " + normalizedSlug);
        }

        Category category = categoryRepository.findByIdAndActiveTrue(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CATEGORY_NOT_FOUND,
                        "Active category not found with id: " + request.getCategoryId()
                ));

        Product product = Product.builder()
                .sku(normalizedSku)
                .name(request.getName().trim())
                .slug(normalizedSlug)
                .description(normalizeDescription(request.getDescription()))
                .price(request.getPrice())
                .category(category)
                .build();

        try {
            Product savedProduct = productRepository.saveAndFlush(product); // để lỗi unique xảy ra bên trong try/catch vì có @Transactional nên thêm flush
            return productMapper.toResponse(savedProduct);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(
                    ErrorCode.PRODUCT_ALREADY_EXISTS,
                    "Product SKU or slug already exists",
                    exception
            );
        }
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.PRODUCT_NOT_FOUND,
                        "Product not found with id: " + id
                ));

        // xử lý optimistic locking
        if (!product.getVersion().equals(request.getVersion())) {
            throw new ConflictException(
                    ErrorCode.PRODUCT_VERSION_CONFLICT,
                    "Product has been modified by another request"
            );
        }

        Category category = categoryRepository.findByIdAndActiveTrue(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CATEGORY_NOT_FOUND,
                        "Active category not found with id: " + request.getCategoryId()
                ));

        String normalizedSku = request.getSku().trim().toUpperCase(Locale.ROOT);
        String normalizedSlug = request.getSlug().trim().toLowerCase(Locale.ROOT);

       if(productRepository.existsBySkuAndIdNot(normalizedSku, id)) {
           throw new ConflictException(
                   ErrorCode.PRODUCT_SKU_ALREADY_EXISTS,
                   "Product SKU already exists: " + normalizedSku
           );
       }

       if(productRepository.existsBySlugAndIdNot(normalizedSlug, id)) {
           throw new ConflictException(
                   ErrorCode.PRODUCT_SLUG_ALREADY_EXISTS,
                   "Product slug already exists: " + normalizedSlug
           );
       }

        product.updateDetails(normalizedSku,
                request.getName().trim(),
                normalizedSlug,
                normalizeDescription(request.getDescription()),
                request.getPrice(),
                category
        );

        try{
            productRepository.flush(); // tăng version trước khi trả response
            return productMapper.toResponse(product);
        } catch (OptimisticLockingFailureException exception) { // bắt lỗi optimistic locking -> kiểm tra lúc update
            throw new ConflictException(
                    ErrorCode.PRODUCT_VERSION_CONFLICT,
                    "Product has been modified by another request",
                    exception
            );
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(
                    ErrorCode.PRODUCT_ALREADY_EXISTS,
                    "Product SKU or slug already exists",
                    exception
            );
        }
    }

    @Override
    @Transactional
    public ProductResponse changeStatus(UUID id, UpdateProductStatusRequest request) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.PRODUCT_NOT_FOUND,
                        "Product not found with id: " + id
                ));

        // xử lý optimistic locking
        if(!product.getVersion().equals(request.getVersion())) {
            throw new ConflictException(
                    ErrorCode.PRODUCT_VERSION_CONFLICT,
                    "Product has been modified by another request"
            );
        }

        if(product.getStatus() == request.getStatus()) {
            return productMapper.toResponse(product);
        }

        product.setStatus(request.getStatus());

        try {
            productRepository.flush(); // chỉ cần flush() vì product đang được managed
            return productMapper.toResponse(product);
        } catch (OptimisticLockingFailureException exception) {
            throw new ConflictException(
                    ErrorCode.PRODUCT_VERSION_CONFLICT,
                    "Product has been modified by another request",
                    exception
            );
        }
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        productRepository.softDeleteById(id, ProductStatus.INACTIVE, Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findByIdAndStatusAndDeletedAtIsNullAndCategory_ActiveTrue(id, ProductStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.PRODUCT_NOT_FOUND,
                        "Active product not found with id: " + id
                ));

        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(ProductSearchRequest request) {
        if (request.getMinPrice() != null
                && request.getMaxPrice() != null
                && request.getMinPrice().compareTo(request.getMaxPrice()) > 0
        ) {
            throw new InvalidRequestException(
                    ErrorCode.INVALID_PRICE_RANGE,
                    "Minimum price must not be greater than maximum price"
            );
        }

        String normalizedKeyword = normalizeKeyword(
                request.getKeyword()
        );

        int pageNumber = 0; // Spring tính trang đầu tiên là 0
        if (request.getCurrentPage() > 0) {
            pageNumber = request.getCurrentPage() - 1;
        }

        Pageable pageable = PageRequest.of(pageNumber, request.getPageSize(), createSort(request.getSortBy()));

        Page<ProductResponse> result = productRepository
                .searchPublicProducts(
                        ProductStatus.ACTIVE,
                        normalizedKeyword,
                        request.getCategoryId(),
                        request.getMinPrice(),
                        request.getMaxPrice(),
                        pageable
                )
                .map(productMapper::toResponse);

        return PageResponse.from(result);
    }

    private Sort createSort(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return Sort.by(Sort.Order.desc("createdAt"));
        }

        List<Sort.Order> orders = new ArrayList<>();

        // name:asc,price:desc
        for (String expression : sortBy.split(",")) {
            String[] parts = expression.trim().split(":", -1);

            if (parts.length != 2) {
                throw invalidSort(sortBy);
            }

            String requestedField = parts[0].trim();
            String requestedDirection = parts[1].trim().toLowerCase(Locale.ROOT);

            String entityField = ALLOWED_SORT_FIELDS.get(requestedField);

            if (entityField == null) {
                throw invalidSort(sortBy);
            }

            Sort.Direction direction = switch (requestedDirection) {
                case "asc" -> Sort.Direction.ASC;
                case "desc" -> Sort.Direction.DESC;
                default -> throw invalidSort(sortBy);
            };

            orders.add(new Sort.Order(direction, entityField));
        }

        return Sort.by(orders);
    }

    private InvalidRequestException invalidSort(String sortBy) {
        return new InvalidRequestException(
                ErrorCode.INVALID_SORT_PARAMETER,
                "Invalid sort parameter: " + sortBy
        );
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "";
        }

        return keyword.trim().toLowerCase(Locale.ROOT);
    }
}
