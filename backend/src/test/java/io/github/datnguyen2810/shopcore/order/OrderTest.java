package io.github.datnguyen2810.shopcore.order;

import io.github.datnguyen2810.shopcore.common.exception.ConflictException;
import io.github.datnguyen2810.shopcore.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-21T00:00:00Z");

    @Test
    void create_withItemSnapshot_calculatesTotalsAndStartsPendingPayment() {
        OrderItem item = createItem(UUID.randomUUID());

        Order order = createOrder(List.of(item));
        OrderStatusHistory history = OrderStatusHistory.created(order);

        assertThat(order.getItems()).containsExactly(item);
        assertThat(item.getOrder()).isSameAs(order);
        assertThat(item.getLineTotal()).isEqualByComparingTo("25.00");
        assertThat(order.getSubtotal()).isEqualByComparingTo("25.00");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("20.00");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(history.getFromStatus()).isNull();
        assertThat(history.getToStatus()).isEqualTo(order.getStatus());
        assertThat(history.getChangedBy()).isEqualTo(order.getUserId());
        assertThat(history.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void create_withDuplicateProduct_rejectsOrderBeforeAttachingItems() {
        UUID productId = UUID.randomUUID();
        OrderItem first = createItem(productId);
        OrderItem duplicate = createItem(productId);

        assertThatThrownBy(() -> createOrder(List.of(first, duplicate)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Product must appear only once per order");
        assertThat(first.getOrder()).isNull();
        assertThat(duplicate.getOrder()).isNull();
    }

    @Test
    void changeStatus_whenCancelled_returnsHistoryAndRejectsRepeatedTransition() {
        Order order = createOrder(List.of(createItem(UUID.randomUUID())));
        Instant cancelledAt = CREATED_AT.plusSeconds(10);
        UUID actor = order.getUserId();

        OrderStatusHistory history = order.changeStatus(OrderStatus.CANCELLED, actor, "Customer requested", cancelledAt);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getUpdatedAt()).isEqualTo(cancelledAt);
        assertThat(history.getFromStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(history.getToStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(history.getChangedBy()).isEqualTo(actor);
        assertThat(history.getCreatedAt()).isEqualTo(cancelledAt);
        assertThat(history.getNote()).isEqualTo("Customer requested");

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.CANCELLED, actor, null, cancelledAt.plusSeconds(1)))
                .isInstanceOfSatisfying(ConflictException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_INVALID_STATUS_TRANSITION));
        assertThat(order.getUpdatedAt()).isEqualTo(cancelledAt);
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING_PAYMENT, CONFIRMED",
            "PENDING_PAYMENT, CANCELLED",
            "PENDING_PAYMENT, EXPIRED",
            "CONFIRMED, PROCESSING",
            "PROCESSING, SHIPPING",
            "SHIPPING, COMPLETED"
    })
    void validateTransition_withAllowedStates_acceptsTransition(OrderStatus from, OrderStatus to) {
        OrderStatePolicy.validateTransition(from, to);
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING_PAYMENT, SHIPPING",
            "CONFIRMED, CANCELLED",
            "PROCESSING, COMPLETED",
            "SHIPPING, CANCELLED",
            "COMPLETED, PROCESSING",
            "CANCELLED, CONFIRMED",
            "EXPIRED, CONFIRMED"
    })
    void validateTransition_withDisallowedStates_rejectsTransition(OrderStatus from, OrderStatus to) {
        assertThatThrownBy(() -> OrderStatePolicy.validateTransition(from, to))
                .isInstanceOfSatisfying(ConflictException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_INVALID_STATUS_TRANSITION));
    }

    @Test
    void generate_withCheckoutInstant_includesUtcDateAndRandomSuffix() {
        assertThat(new OrderNumberGenerator().generate(CREATED_AT)).matches("SC-20261121-[A-Z0-9]{6}");
    }

    private Order createOrder(List<OrderItem> items) {
        return Order.create(
                UUID.randomUUID(),
                "SC-20261121-ABC123",
                new Order.ShippingAddress("Test Customer", "0901234567", "1 Test Street", "Ward 1", "District 1", "HCMC"),
                items,
                "WELCOME",
                new BigDecimal("5.00"),
                "checkout-key",
                "a".repeat(64),
                CREATED_AT,
                CREATED_AT.plusSeconds(900)
        );
    }

    private OrderItem createItem(UUID productId) {
        return OrderItem.create(productId, "SKU-001", "Phone Case", new BigDecimal("12.50"), 2);
    }
}
