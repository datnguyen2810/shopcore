package io.github.datnguyen2810.shopcore.product.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CreateProductRequest {

    @NotBlank(message = "SKU is required")
    @Size(max = 80, message = "SKU must not exceed 80 characters")
    private String sku;

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @NotBlank(message = "Slug is required")
    @Pattern(
            regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
            message = "Slug must contain only lowercase letters, numbers, and hyphens"
    )
    @Size(max = 220, message = "Slug must not exceed 220 characters")
    private String slug;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", message = "Price must be greater than or equal to 0")
    @Digits(
            integer = 17,
            fraction = 2,
            message = "Price must have at most 17 integer digits and 2 decimal places"
    )
    private BigDecimal price;

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

//    @Valid
////    @Builder.Default
////    private List<ProductImageRequest> images = new ArrayList<>();


}
