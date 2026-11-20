package io.github.datnguyen2810.shopcore.voucher;

import io.github.datnguyen2810.shopcore.common.exception.ConflictException;
import io.github.datnguyen2810.shopcore.common.exception.ErrorCode;
import io.github.datnguyen2810.shopcore.common.exception.InvalidRequestException;
import io.github.datnguyen2810.shopcore.common.exception.ResourceNotFoundException;
import io.github.datnguyen2810.shopcore.common.response.PageResponse;
import io.github.datnguyen2810.shopcore.voucher.dto.CreateVoucherRequest;
import io.github.datnguyen2810.shopcore.voucher.dto.UpdateVoucherRequest;
import io.github.datnguyen2810.shopcore.voucher.dto.VoucherResponse;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {
    private final VoucherRepository voucherRepository;
    private final VoucherMapper voucherMapper;

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('VOUCHER_MANAGE')")
    public VoucherResponse createVoucher(CreateVoucherRequest request) {
        Voucher voucher;

        try {
            voucher = Voucher.create(
                    request.getCode(),
                    request.getDiscountType(),
                    request.getDiscountValue(),
                    request.getMinimumOrderValue(),
                    request.getMaximumDiscount(),
                    request.getUsageLimit(),
                    request.getStartsAt(),
                    request.getExpiresAt(),
                    request.getActive()
            );
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException(
                    ErrorCode.VOUCHER_INVALID_CONFIGURATION,
                    exception.getMessage()
            );
        }

        Voucher savedVoucher;
        try {
            savedVoucher = voucherRepository.saveAndFlush(voucher); // đưa lỗi unique code vào trong try catch
        } catch (DataIntegrityViolationException exception) {
            if (violatesVoucherCodeConstraint(exception)) {
                ErrorCode code = ErrorCode.VOUCHER_CODE_ALREADY_EXISTS;
                throw new ConflictException(code, code.getDefaultMessage(), exception);
            }
            throw exception;
        }

        return voucherMapper.toVoucherResponse(savedVoucher);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VOUCHER_MANAGE')")
    public PageResponse<VoucherResponse> getVouchers(int currentPage, int pageSize) {
        Pageable pageable = PageRequest.of(currentPage, pageSize, Sort.by("code"));

        Page<VoucherResponse> result = voucherRepository.findAll(pageable)
                .map(voucherMapper::toVoucherResponse);

        return PageResponse.from(result);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VOUCHER_MANAGE')")
    public VoucherResponse getVoucherById(UUID id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.VOUCHER_NOT_FOUND,
                        ErrorCode.VOUCHER_NOT_FOUND.getDefaultMessage()
                ));

        return voucherMapper.toVoucherResponse(voucher);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('VOUCHER_MANAGE')")
    public VoucherResponse updateVoucher(UUID id, UpdateVoucherRequest request) {
        Voucher voucher = voucherRepository.findByIdForUpdate(id) // khóa dòng
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.VOUCHER_NOT_FOUND,
                        ErrorCode.VOUCHER_NOT_FOUND.getDefaultMessage()
                ));

        try {
            voucher.update(
                    request.getDiscountType(),
                    request.getDiscountValue(),
                    request.getMinimumOrderValue(),
                    request.getMaximumDiscount(),
                    request.getUsageLimit(),
                    request.getStartsAt(),
                    request.getExpiresAt(),
                    request.getActive()
            );
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException(
                    ErrorCode.VOUCHER_INVALID_CONFIGURATION,
                    exception.getMessage()
            );
        }

        return voucherMapper.toVoucherResponse(voucher);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('VOUCHER_MANAGE')")
    public void deleteVoucher(UUID id) {
        if (voucherRepository.deactivateById(id) == 0) {
            ErrorCode code = ErrorCode.VOUCHER_NOT_FOUND;
            throw new ResourceNotFoundException(code, code.getDefaultMessage());
        }
    }

    private boolean violatesVoucherCodeConstraint(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && "uk_vouchers_code".equals(violation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
