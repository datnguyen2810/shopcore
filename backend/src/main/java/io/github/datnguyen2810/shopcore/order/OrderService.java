package io.github.datnguyen2810.shopcore.order;

import io.github.datnguyen2810.shopcore.common.response.PageResponse;
import io.github.datnguyen2810.shopcore.order.dto.CreateOrderRequest;
import io.github.datnguyen2810.shopcore.order.dto.OrderResponse;
import io.github.datnguyen2810.shopcore.order.dto.OrderSummaryResponse;
import io.github.datnguyen2810.shopcore.order.dto.UpdateOrderStatusRequest;

import java.util.UUID;

public interface OrderService {
    OrderResponse createOrder(UUID userId, String idempotencyKey, CreateOrderRequest request);

    PageResponse<OrderSummaryResponse> getMyOrders(UUID userId, OrderStatus status, int currentPage, int pageSize);

    OrderResponse getMyOrder(UUID userId, UUID orderId);

    OrderResponse cancelMyOrder(UUID userId, UUID orderId, String reason);

    // ===== Admin =====

    PageResponse<OrderSummaryResponse> getOrders(OrderStatus status, UUID userId, String orderNumber,
                                                 int currentPage, int pageSize);

    OrderResponse getOrder(UUID orderId);

    OrderResponse updateOrderStatus(UUID adminId, UUID orderId, UpdateOrderStatusRequest request);
}
