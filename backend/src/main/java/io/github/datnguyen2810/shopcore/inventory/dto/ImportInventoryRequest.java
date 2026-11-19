package io.github.datnguyen2810.shopcore.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ImportInventoryRequest {

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;

    // Có thể bỏ qua version khi nhập kho lần đầu cho sản phẩm.
    @PositiveOrZero(message = "Version must be greater than or equal to 0")
    @NotNull(message = "Version is required")
    private Long version;
}
