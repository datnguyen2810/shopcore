package io.github.datnguyen2810.shopcore.order.dto;

import io.github.datnguyen2810.shopcore.order.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Bản tóm tắt cho danh sách đơn: không có items/address để tránh N+1 query khi phân trang. */
@Getter
@Builder
public class OrderSummaryResponse {

    private final UUID id;
    private final UUID userId;
    private final String orderNumber;
    private final OrderStatus status;
    private final BigDecimal subtotal;
    private final BigDecimal discountAmount;
    private final BigDecimal totalAmount;
    private final String voucherCode;
    private final Instant paymentExpiresAt;
    private final Instant createdAt;
}
