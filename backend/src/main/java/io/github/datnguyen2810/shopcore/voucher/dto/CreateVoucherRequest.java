package io.github.datnguyen2810.shopcore.voucher.dto;

import io.github.datnguyen2810.shopcore.voucher.DiscountType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class CreateVoucherRequest {

    @NotBlank(message = "Voucher code is required")
    @Size(max = 80, message = "Voucher code must not exceed 80 characters")
    private String code;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @Positive(message = "Discount value must be positive")
    @Digits(integer = 17, fraction = 2)
    private BigDecimal discountValue;

    @NotNull(message = "Minimum order value is required")
    @PositiveOrZero(message = "Minimum order value must be non-negative")
    @Digits(integer = 17, fraction = 2)
    private BigDecimal minimumOrderValue = BigDecimal.ZERO;

    @Positive(message = "Maximum discount must be positive")
    @Digits(integer = 17, fraction = 2)
    private BigDecimal maximumDiscount;

    @NotNull(message = "Usage limit is required")
    @Positive(message = "Usage limit must be positive")
    private Integer usageLimit;

    @NotNull(message = "Start time is required")
    private Instant startsAt;

    @NotNull(message = "Expiration time is required")
    private Instant expiresAt;

    @NotNull(message = "Active status is required")
    private Boolean active = true;
}
