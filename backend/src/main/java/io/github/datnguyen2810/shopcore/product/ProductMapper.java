package io.github.datnguyen2810.shopcore.product;

import io.github.datnguyen2810.shopcore.product.dto.ProductImageResponse;
import io.github.datnguyen2810.shopcore.product.dto.ProductResponse;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(product.getPrice())
                .status(product.getStatus())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .images(
                        product.getProductImages()
                                .stream()
                                .sorted(Comparator.comparingInt(ProductImage::getDisplayOrder))
                                .map(this::toImageResponse)
                                .toList()
                )
                .version(product.getVersion())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private ProductImageResponse toImageResponse(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .url(image.getUrl())
                .displayOrder(image.getDisplayOrder())
                .build();
    }
}
