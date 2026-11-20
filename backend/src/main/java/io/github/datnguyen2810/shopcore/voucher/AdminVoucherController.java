package io.github.datnguyen2810.shopcore.voucher;

import io.github.datnguyen2810.shopcore.common.response.ApiResponse;
import io.github.datnguyen2810.shopcore.common.response.PageResponse;
import io.github.datnguyen2810.shopcore.voucher.dto.CreateVoucherRequest;
import io.github.datnguyen2810.shopcore.voucher.dto.UpdateVoucherRequest;
import io.github.datnguyen2810.shopcore.voucher.dto.VoucherResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/vouchers")
public class AdminVoucherController {
    private final VoucherService voucherService;

    @PostMapping
    public ResponseEntity<ApiResponse<VoucherResponse>> createVoucher(@Valid @RequestBody CreateVoucherRequest request){
        VoucherResponse createdVoucher = voucherService.createVoucher(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(
                        HttpStatus.CREATED,
                        "Voucher created successfully",
                        createdVoucher
                ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VoucherResponse>>> getVouchers(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be greater than or equal to 0")
            int currentPage,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Page size must be greater than or equal to 1")
            @Max(value = 100, message = "Page size must not exceed 100")
            int pageSize
    ) {
        PageResponse<VoucherResponse> vouchers = voucherService.getVouchers(currentPage, pageSize);

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Vouchers retrieved successfully", vouchers));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherResponse>> getVoucherById(@PathVariable UUID id) {
        VoucherResponse voucher = voucherService.getVoucherById(id);

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Voucher found successfully", voucher));
    }

    @PutMapping("{id}")
    public ResponseEntity<ApiResponse<VoucherResponse>> updateVoucher(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVoucherRequest request
    ){
        VoucherResponse voucher = voucherService.updateVoucher(id, request);

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Voucher updated successfully", voucher));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVoucher(@PathVariable UUID id) {
        voucherService.deleteVoucher(id);

        return ResponseEntity.ok(ApiResponse.message(HttpStatus.OK, "Voucher deactivated successfully"));
    }
}
