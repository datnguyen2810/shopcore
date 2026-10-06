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
public class ProductSearchRequest {

    private String keyword;

    private UUID categoryId;

    @DecimalMin(
            value = "0.00",
            message = "Minimum price must be greater than or equal to 0"
    )
    private BigDecimal minPrice;

    @DecimalMin(
            value = "0.00",
            message = "Maximum price must be greater than or equal to 0"
    )
    private BigDecimal maxPrice;

    @PositiveOrZero(message = "Page must be greater than or equal to 0")
    private int currentPage = 0;

    @Min(value = 1, message = "Page size must be greater than or equal to 1")
    @Max(value = 100, message = "Page size must not exceed 100")
    private int pageSize = 20;

    @Pattern(
            regexp = "^[a-zA-Z][a-zA-Z0-9]*:(asc|desc)"
                    + "(,[a-zA-Z][a-zA-Z0-9]*:(asc|desc))*$",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Sort must have format field:asc or field:desc"
    )
    private String sortBy;
}
