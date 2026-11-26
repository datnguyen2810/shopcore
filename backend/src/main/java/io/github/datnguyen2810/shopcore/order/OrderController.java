package io.github.datnguyen2810.shopcore.order;

import io.github.datnguyen2810.shopcore.common.response.ApiResponse;
import io.github.datnguyen2810.shopcore.common.response.PageResponse;
import io.github.datnguyen2810.shopcore.order.dto.CancelOrderRequest;
import io.github.datnguyen2810.shopcore.order.dto.CreateOrderRequest;
import io.github.datnguyen2810.shopcore.order.dto.OrderResponse;
import io.github.datnguyen2810.shopcore.order.dto.OrderSummaryResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        OrderResponse order = orderService.createOrder(userId, idempotencyKey, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED, "Order created successfully", order));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> getMyOrders(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be greater than or equal to 0")
            int currentPage,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Page size must be greater than or equal to 1")
            @Max(value = 100, message = "Page size must not exceed 100")
            int pageSize,

            @RequestParam(required = false) OrderStatus status,
            @AuthenticationPrincipal Jwt jwt
    ) {
        // Luôn lấy userId từ token, không nhận từ query param -> user không xem được đơn của người khác.
        UUID userId = UUID.fromString(jwt.getSubject());
        PageResponse<OrderSummaryResponse> orders = orderService.getMyOrders(userId, status, currentPage, pageSize);

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Orders retrieved successfully", orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getMyOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        OrderResponse order = orderService.getMyOrder(userId, id);

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Order retrieved successfully", order));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelMyOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) CancelOrderRequest request // body không bắt buộc, chỉ chứa lý do huỷ
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String reason = request == null ? null : request.getReason();
        OrderResponse order = orderService.cancelMyOrder(userId, id, reason);

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Order cancelled successfully", order));
    }
}
