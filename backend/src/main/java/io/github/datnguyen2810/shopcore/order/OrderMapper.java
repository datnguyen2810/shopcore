package io.github.datnguyen2810.shopcore.order;

import io.github.datnguyen2810.shopcore.order.dto.OrderItemResponse;
import io.github.datnguyen2810.shopcore.order.dto.OrderResponse;
import io.github.datnguyen2810.shopcore.order.dto.OrderSummaryResponse;
import io.github.datnguyen2810.shopcore.order.dto.ShippingAddressRequest;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class OrderMapper {

    public OrderSummaryResponse toOrderSummaryResponse(Order order) {
        return OrderSummaryResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .voucherCode(order.getVoucherCode())
                .paymentExpiresAt(order.getPaymentExpiresAt())
                .createdAt(order.getCreatedAt())
                .build();
    }

    public OrderResponse toOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .voucherCode(order.getVoucherCode())
                .shippingAddress(toShippingAddressResponse(order.getShippingAddress()))
                .items(order.getItems().stream()
                        .sorted(Comparator.comparing(OrderItem::getProductId))
                        .map(this::toOrderItemResponse)
                        .toList())
                .paymentExpiresAt(order.getPaymentExpiresAt())
                .version(order.getVersion())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public Order.ShippingAddress toShippingAddress(ShippingAddressRequest request) {
        return new Order.ShippingAddress(
                request.getRecipientName().strip(),
                request.getRecipientPhone().strip(),
                request.getAddressLine().strip(),
                request.getWard().strip(),
                request.getDistrict().strip(),
                request.getCity().strip()
        );
    }

    private OrderResponse.ShippingAddressResponse toShippingAddressResponse(Order.ShippingAddress address) {
        return OrderResponse.ShippingAddressResponse.builder()
                .recipientName(address.getRecipientName())
                .recipientPhone(address.getRecipientPhone())
                .addressLine(address.getAddressLine())
                .ward(address.getWard())
                .district(address.getDistrict())
                .city(address.getCity())
                .build();
    }

    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .sku(item.getSku())
                .productName(item.getProductName())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .lineTotal(item.getLineTotal())
                .build();
    }
}
