package io.github.datnguyen2810.shopcore.voucher.dto;

import io.github.datnguyen2810.shopcore.voucher.DiscountType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class VoucherResponse {

    private final UUID id;
    private final String code;
    private final DiscountType discountType;
    private final BigDecimal discountValue;
    private final BigDecimal minimumOrderValue;
    private final BigDecimal maximumDiscount;
    private final int usageLimit;
    private final int usedCount;
    private final Instant startsAt;
    private final Instant expiresAt;
    private final boolean active;
}
