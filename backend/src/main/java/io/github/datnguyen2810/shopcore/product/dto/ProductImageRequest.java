package io.github.datnguyen2810.shopcore.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductImageRequest {

    @NotBlank(message = "Image URL is required")
    private String url;

    @PositiveOrZero(message = "Display order must be greater than or equal to 0")
    private int displayOrder;
}
