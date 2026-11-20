package io.github.datnguyen2810.shopcore.voucher;

import io.github.datnguyen2810.shopcore.voucher.dto.VoucherResponse;
import org.springframework.stereotype.Component;

@Component
public class VoucherMapper {
    public VoucherResponse toVoucherResponse(Voucher voucher) {
        return VoucherResponse.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .minimumOrderValue(voucher.getMinimumOrderValue())
                .maximumDiscount(voucher.getMaximumDiscount())
                .usageLimit(voucher.getUsageLimit())
                .usedCount(voucher.getUsedCount())
                .startsAt(voucher.getStartsAt())
                .expiresAt(voucher.getExpiresAt())
                .active(voucher.isActive())
                .build();
    }


}
