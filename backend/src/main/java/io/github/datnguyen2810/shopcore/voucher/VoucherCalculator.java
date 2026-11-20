package io.github.datnguyen2810.shopcore.voucher;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Component
public class VoucherCalculator {

    /** Tính số tiền giảm sau khi bên gọi đã kiểm tra voucher đủ điều kiện áp dụng. */
    public BigDecimal calculateDiscount(Voucher voucher, BigDecimal subtotal) {
        Objects.requireNonNull(voucher, "Voucher is required");
        Objects.requireNonNull(subtotal, "Subtotal is required");
        if (subtotal.signum() < 0) {
            throw new IllegalArgumentException("Subtotal must be non-negative");
        }

        BigDecimal orderValue = subtotal.setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal discount = switch (voucher.getDiscountType()) {
            case PERCENTAGE -> orderValue.multiply(voucher.getDiscountValue()).movePointLeft(2);
            case FIXED_AMOUNT -> voucher.getDiscountValue();
        };

        BigDecimal maximumDiscount = voucher.getMaximumDiscount();
        if (maximumDiscount != null) {
            discount = discount.min(maximumDiscount);
        }

        return discount.min(orderValue).setScale(2, RoundingMode.HALF_UP);
    }
}
