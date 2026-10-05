package io.github.datnguyen2810.shopcore.product.dto;

import io.github.datnguyen2810.shopcore.product.ProductStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ProductResponse {
    private final UUID id;
    private final String sku;
    private final String name;
    private final String slug;
    private final String description;
    private final BigDecimal price;
    private final ProductStatus status;

    private final UUID categoryId;
    private final String categoryName;

    private final List<ProductImageResponse> images;

    private final Long version;
    private final Instant createdAt;
    private final Instant updatedAt;
}
