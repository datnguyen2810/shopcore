package io.github.datnguyen2810.shopcore.order;

import io.github.datnguyen2810.shopcore.common.response.ApiResponse;
import io.github.datnguyen2810.shopcore.common.response.PageResponse;
import io.github.datnguyen2810.shopcore.order.dto.OrderResponse;
import io.github.datnguyen2810.shopcore.order.dto.OrderSummaryResponse;
import io.github.datnguyen2810.shopcore.order.dto.UpdateOrderStatusRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// /api/v1/admin/** yêu cầu role ADMIN (SecurityConfig); quyền chi tiết kiểm tra bằng @PreAuthorize ở service
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {
    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> getOrders(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be greater than or equal to 0")
            int currentPage,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Page size must be greater than or equal to 1")
            @Max(value = 100, message = "Page size must not exceed 100")
            int pageSize,

            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String orderNumber
    ) {
        PageResponse<OrderSummaryResponse> orders =
                orderService.getOrders(status, userId, orderNumber, currentPage, pageSize);

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Orders retrieved successfully", orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable UUID id) {
        OrderResponse order = orderService.getOrder(id);

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Order retrieved successfully", order));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        OrderResponse order = orderService.updateOrderStatus(adminId, id, request);

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Order status updated successfully", order));
    }
}
