package io.github.datnguyen2810.shopcore.category.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CategorySearchRequest {

    @Size(max = 150, message = "Keyword must not exceed 150 characters")
    private String keyword;

    @NotNull(message = "Active must be true or false")
    private Boolean active = true;

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
