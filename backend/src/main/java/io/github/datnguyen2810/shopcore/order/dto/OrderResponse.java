package io.github.datnguyen2810.shopcore.order.dto;

import io.github.datnguyen2810.shopcore.order.OrderStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class OrderResponse {

    private final UUID id;
    private final UUID userId;
    private final String orderNumber;
    private final OrderStatus status;
    private final BigDecimal subtotal;
    private final BigDecimal discountAmount;
    private final BigDecimal totalAmount;
    private final String voucherCode;
    private final ShippingAddressResponse shippingAddress;
    @Singular
    private final List<OrderItemResponse> items;
    private final Instant paymentExpiresAt;
    private final Long version;
    private final Instant createdAt;
    private final Instant updatedAt;

    @Getter
    @Builder
    public static class ShippingAddressResponse {

        private final String recipientName;
        private final String recipientPhone;
        private final String addressLine;
        private final String ward;
        private final String district;
        private final String city;
    }
}
