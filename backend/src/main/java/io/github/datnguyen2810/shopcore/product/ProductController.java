package io.github.datnguyen2810.shopcore.product;

import io.github.datnguyen2810.shopcore.common.response.ApiResponse;
import io.github.datnguyen2810.shopcore.common.response.PageResponse;
import io.github.datnguyen2810.shopcore.product.dto.ProductResponse;
import io.github.datnguyen2810.shopcore.product.dto.ProductSearchRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(
            @Valid @ModelAttribute ProductSearchRequest request
    ) {
        PageResponse<ProductResponse> products = productService.searchProducts(request);

        ApiResponse<PageResponse<ProductResponse>> response = ApiResponse.of(HttpStatus.OK, "Products retrieved successfully", products);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable UUID id) {
        ProductResponse product = productService.getProductById(id);

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Product retrieved successfully", product));
    }
}
