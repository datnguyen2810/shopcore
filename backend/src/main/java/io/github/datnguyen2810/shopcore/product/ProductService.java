package io.github.datnguyen2810.shopcore.product;

import io.github.datnguyen2810.shopcore.common.response.PageResponse;
import io.github.datnguyen2810.shopcore.product.dto.*;
import jakarta.validation.Valid;

import java.util.UUID;

public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse updateProduct(UUID id, UpdateProductRequest request);

    ProductResponse changeStatus(UUID id, @Valid UpdateProductStatusRequest request);

    void deleteById(UUID id);

    ProductResponse getProductById(UUID id);

    PageResponse<ProductResponse> searchProducts(@Valid ProductSearchRequest request);
}
