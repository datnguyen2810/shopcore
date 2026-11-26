package io.github.datnguyen2810.shopcore.voucher;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "vouchers")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, updatable = false, length = 80)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 30)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "minimum_order_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal minimumOrderValue;

    @Column(name = "maximum_discount", precision = 19, scale = 2)
    private BigDecimal maximumDiscount;

    @Column(name = "usage_limit", nullable = false)
    private int usageLimit;

    @Column(name = "used_count", nullable = false, updatable = false)
    private int usedCount;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "active", nullable = false)
    private boolean active;

    public static Voucher create(
            String code,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal minimumOrderValue,
            BigDecimal maximumDiscount,
            int usageLimit,
            Instant startsAt,
            Instant expiresAt,
            boolean active
    ) {
        String trimmedCode = Objects.requireNonNull(code, "Voucher code is required").strip();
        if (trimmedCode.isEmpty() || trimmedCode.length() > 80) {
            throw new IllegalArgumentException("Voucher code must contain between 1 and 80 characters");
        }

        Voucher voucher = new Voucher();
        voucher.code = trimmedCode;
        voucher.update(discountType, discountValue, minimumOrderValue, maximumDiscount,
                usageLimit, startsAt, expiresAt, active);
        return voucher;
    }

    public void update(
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal minimumOrderValue,
            BigDecimal maximumDiscount,
            int usageLimit,
            Instant startsAt,
            Instant expiresAt,
            boolean active
    ) {
        Objects.requireNonNull(discountType, "Discount type is required");
        Objects.requireNonNull(startsAt, "Start time is required");
        Objects.requireNonNull(expiresAt, "Expiration time is required");

        BigDecimal validDiscountValue = validateAmount(discountValue, "Discount value");
        BigDecimal validMinimumOrderValue = validateAmount(minimumOrderValue, "Minimum order value");
        BigDecimal validMaximumDiscount = maximumDiscount == null
                ? null : validateAmount(maximumDiscount, "Maximum discount");

        if (validDiscountValue.signum() <= 0) {
            throw new IllegalArgumentException("Discount value must be positive");
        }
        if (discountType == DiscountType.PERCENTAGE && validDiscountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Percentage discount must not exceed 100");
        }
        if (validMinimumOrderValue.signum() < 0) {
            throw new IllegalArgumentException("Minimum order value must be non-negative");
        }
        if (validMaximumDiscount != null && validMaximumDiscount.signum() <= 0) {
            throw new IllegalArgumentException("Maximum discount must be positive");
        }
        if (usageLimit <= 0 || usageLimit < usedCount) {
            throw new IllegalArgumentException("Usage limit must be positive and at least the used count");
        }
        if (!expiresAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("Expiration time must be after the start time");
        }

        this.discountType = discountType;
        this.discountValue = validDiscountValue;
        this.minimumOrderValue = validMinimumOrderValue;
        this.maximumDiscount = validMaximumDiscount;
        this.usageLimit = usageLimit;
        this.startsAt = startsAt;
        this.expiresAt = expiresAt;
        this.active = active;
    }

    /** Voucher còn áp dụng được cho đơn có subtotal này tại thời điểm now hay không. */
    public boolean isUsableFor(BigDecimal subtotal, Instant now) {
        return active
                && !now.isBefore(startsAt)
                && now.isBefore(expiresAt)
                && subtotal.compareTo(minimumOrderValue) >= 0
                && usedCount < usageLimit;
    }

    private static BigDecimal validateAmount(BigDecimal value, String field) {
        BigDecimal amount = Objects.requireNonNull(value, field + " is required").stripTrailingZeros();
        if (amount.scale() > 2 || (long) amount.precision() - amount.scale() > 17) {
            throw new IllegalArgumentException(field + " must fit NUMERIC(19, 2)");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }
}
