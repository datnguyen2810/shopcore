package io.github.datnguyen2810.shopcore.inventory.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdjustInventoryRequest {

    @NotNull(message = "Quantity delta is required")
    private Integer quantityDelta;

    @NotBlank(message = "Adjustment note is required")
    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;

    @NotNull(message = "Version is required")
    @PositiveOrZero(message = "Version must be greater than or equal to 0")
    private Long version;

    @JsonIgnore
    @AssertTrue(message = "Quantity delta must not be zero")
    public boolean isQuantityDeltaNonZero() {
        return quantityDelta == null || quantityDelta != 0;
    }
}
