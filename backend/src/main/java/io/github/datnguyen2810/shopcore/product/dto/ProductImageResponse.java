package io.github.datnguyen2810.shopcore.product.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ProductImageResponse {
    private final UUID id;
    private final String url;
    private final int displayOrder;
}
