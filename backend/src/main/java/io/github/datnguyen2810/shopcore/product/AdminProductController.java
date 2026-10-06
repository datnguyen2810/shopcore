package io.github.datnguyen2810.shopcore.product;

import io.github.datnguyen2810.shopcore.common.response.ApiResponse;
import io.github.datnguyen2810.shopcore.product.dto.CreateProductRequest;
import io.github.datnguyen2810.shopcore.product.dto.ProductResponse;
import io.github.datnguyen2810.shopcore.product.dto.UpdateProductRequest;
import io.github.datnguyen2810.shopcore.product.dto.UpdateProductStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {
    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody CreateProductRequest request){
        ProductResponse createdProduct = productService.createProduct(request);

        ApiResponse<ProductResponse> response = ApiResponse.of(HttpStatus.CREATED, "Product created successfully", createdProduct);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(@PathVariable UUID id, @Valid @RequestBody UpdateProductRequest request) {
        ProductResponse updatedProduct = productService.updateProduct(id, request);

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Product updated successfully", updatedProduct));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ProductResponse>> changeProductStatus(@PathVariable UUID id, @Valid @RequestBody UpdateProductStatusRequest request) {
        ProductResponse updatedProduct = productService.changeStatus(id, request);

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Product status changed successfully", updatedProduct));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        productService.deleteById(id);

        ApiResponse<Void> response = ApiResponse.message(HttpStatus.OK, "Product deleted successfully");
        return ResponseEntity.ok(response);
    }

}
