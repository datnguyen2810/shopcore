package io.github.datnguyen2810.shopcore.order;

import io.github.datnguyen2810.shopcore.common.exception.ConflictException;
import io.github.datnguyen2810.shopcore.common.exception.ErrorCode;

// quyết định đơn có được chuyển trạng thái hay không
public final class OrderStatePolicy {

    private OrderStatePolicy() {
    }

    public static void validateTransition(OrderStatus current, OrderStatus target) {
        boolean allowed = switch (current) {
            case PENDING_PAYMENT -> target == OrderStatus.CONFIRMED
                    || target == OrderStatus.CANCELLED || target == OrderStatus.EXPIRED;
            case CONFIRMED -> target == OrderStatus.PROCESSING;
            case PROCESSING -> target == OrderStatus.SHIPPING;
            case SHIPPING -> target == OrderStatus.COMPLETED;
            case COMPLETED, CANCELLED, EXPIRED -> false;
        };

        if (!allowed) {
            throw new ConflictException(
                    ErrorCode.ORDER_INVALID_STATUS_TRANSITION,
                    "Order cannot transition from " + current + " to " + target
            );
        }
    }
}
