package io.github.datnguyen2810.shopcore.product.dto;

import io.github.datnguyen2810.shopcore.product.ProductStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateProductStatusRequest {

    @NotNull(message = "Status is required")
    private ProductStatus status;

    @NotNull(message = "Version is required")
    @PositiveOrZero(message = "Version must be greater than or equal to 0")
    private Long version;
}
