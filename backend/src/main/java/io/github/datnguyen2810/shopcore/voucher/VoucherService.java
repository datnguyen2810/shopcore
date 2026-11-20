package io.github.datnguyen2810.shopcore.voucher;

import io.github.datnguyen2810.shopcore.common.response.PageResponse;
import io.github.datnguyen2810.shopcore.voucher.dto.CreateVoucherRequest;
import io.github.datnguyen2810.shopcore.voucher.dto.UpdateVoucherRequest;
import io.github.datnguyen2810.shopcore.voucher.dto.VoucherResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

public interface VoucherService {
    VoucherResponse createVoucher(CreateVoucherRequest request);

    PageResponse<VoucherResponse> getVouchers(int currentPage, int pageSize);

    VoucherResponse getVoucherById(UUID id);

    VoucherResponse updateVoucher(UUID id, @Valid UpdateVoucherRequest request);

    void deleteVoucher(UUID id);
}
