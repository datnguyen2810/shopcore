package io.github.datnguyen2810.shopcore.order;

import io.github.datnguyen2810.shopcore.cart.Cart;
import io.github.datnguyen2810.shopcore.cart.CartRepository;
import io.github.datnguyen2810.shopcore.common.exception.ConflictException;
import io.github.datnguyen2810.shopcore.common.exception.ErrorCode;
import io.github.datnguyen2810.shopcore.common.exception.InvalidRequestException;
import io.github.datnguyen2810.shopcore.common.exception.ResourceNotFoundException;
import io.github.datnguyen2810.shopcore.common.response.PageResponse;
import io.github.datnguyen2810.shopcore.inventory.InventoryRepository;
import io.github.datnguyen2810.shopcore.inventory.InventoryTransactionRepository;
import io.github.datnguyen2810.shopcore.order.dto.CreateOrderRequest;
import io.github.datnguyen2810.shopcore.order.dto.OrderResponse;
import io.github.datnguyen2810.shopcore.order.dto.OrderSummaryResponse;
import io.github.datnguyen2810.shopcore.order.dto.ShippingAddressRequest;
import io.github.datnguyen2810.shopcore.order.dto.UpdateOrderStatusRequest;
import io.github.datnguyen2810.shopcore.product.Product;
import io.github.datnguyen2810.shopcore.product.ProductRepository;
import io.github.datnguyen2810.shopcore.product.ProductRepository.CheckoutProduct;
import io.github.datnguyen2810.shopcore.product.ProductStatus;
import io.github.datnguyen2810.shopcore.user.User;
import io.github.datnguyen2810.shopcore.user.UserRepository;
import io.github.datnguyen2810.shopcore.voucher.VoucherCalculator;
import io.github.datnguyen2810.shopcore.voucher.VoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final String KEY = "checkout:first-attempt";

    @Mock private OrderRepository orders;
    @Mock private OrderStatusHistoryRepository history;
    @Mock private UserRepository users;
    @Mock private CartRepository carts;
    @Mock private ProductRepository products;
    @Mock private VoucherRepository vouchers;
    @Mock private InventoryRepository inventory;
    @Mock private InventoryTransactionRepository ledger;

    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(
                orders, history, users, carts, products, vouchers, inventory, ledger, new VoucherCalculator(),
                new OrderNumberGenerator(), new OrderMapper(), new OrderProperties(Duration.ofMinutes(15)),
                JsonMapper.builder().build()
        );
    }

    @Test
    void createOrder_whenKeyInvalid_rejectsItBeforeTouchingDatabase() {
        assertThatThrownBy(() -> service.createOrder(USER_ID, null, request()))
                .isInstanceOfSatisfying(InvalidRequestException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_INVALID_IDEMPOTENCY_KEY));
        verifyNoInteractions(users, carts);
    }

    @Test
    void createOrder_whenKeyReused_returnsSavedOrderBeforeReadingCart() {
        stubCheckout();
        when(inventory.reserve(eq(PRODUCT_ID), eq(2), any())).thenReturn(1);

        OrderResponse original = service.createOrder(USER_ID, KEY, request());
        ArgumentCaptor<Order> savedOrder = ArgumentCaptor.forClass(Order.class);
        verify(orders).save(savedOrder.capture());
        when(orders.findByUserIdAndIdempotencyKey(USER_ID, KEY)).thenReturn(Optional.of(savedOrder.getValue()));

        OrderResponse replay = service.createOrder(USER_ID, KEY, request());
        assertThat(replay.getOrderNumber()).isEqualTo(original.getOrderNumber());
        assertThat(replay.getTotalAmount()).isEqualByComparingTo("40.00");
        verify(carts).findByUserId(USER_ID);
        verify(inventory).reserve(eq(PRODUCT_ID), eq(2), any());

        CreateOrderRequest changed = request();
        changed.getShippingAddress().setAddressLine("14 Nguyen Trai");
        assertThatThrownBy(() -> service.createOrder(USER_ID, KEY, changed))
                .isInstanceOfSatisfying(ConflictException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_IDEMPOTENCY_CONFLICT));
        verify(carts).findByUserId(USER_ID);
    }

    @Test
    void getMyOrders_withoutStatus_queriesOwnOrdersNewestFirst() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(orders.findByUserId(eq(USER_ID), pageable.capture())).thenReturn(Page.empty());

        PageResponse<OrderSummaryResponse> page = service.getMyOrders(USER_ID, null, 2, 10);

        assertThat(page.getItems()).isEmpty();
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    @Test
    void getMyOrders_withStatus_filtersByStatus() {
        when(orders.findByUserIdAndStatus(eq(USER_ID), eq(OrderStatus.CANCELLED), any())).thenReturn(Page.empty());

        service.getMyOrders(USER_ID, OrderStatus.CANCELLED, 0, 20);

        verify(orders, never()).findByUserId(any(), any());
    }

    @Test
    void getMyOrder_whenOwned_returnsOrderWithItems() {
        UUID orderId = UUID.randomUUID();
        when(orders.findByIdAndUserId(orderId, USER_ID)).thenReturn(Optional.of(pendingOrder(null)));

        OrderResponse response = service.getMyOrder(USER_ID, orderId);

        assertThat(response.getOrderNumber()).isEqualTo("SC-20261126-ABC123");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("40.00");
    }

    @Test
    void getMyOrder_whenMissingOrOwnedByAnotherUser_returnsNotFound() {
        UUID orderId = UUID.randomUUID();
        when(orders.findByIdAndUserId(orderId, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyOrder(USER_ID, orderId))
                .isInstanceOfSatisfying(ResourceNotFoundException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND));
    }

    @Test
    void cancelMyOrder_whenPendingPayment_cancelsAndReleasesVoucherAndStock() {
        UUID orderId = UUID.randomUUID();
        when(orders.findByIdAndUserIdForUpdate(orderId, USER_ID)).thenReturn(Optional.of(pendingOrder("SALE10")));
        when(inventory.release(eq(PRODUCT_ID), eq(2), any())).thenReturn(1);

        OrderResponse response = service.cancelMyOrder(USER_ID, orderId, "Changed my mind");

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        ArgumentCaptor<OrderStatusHistory> savedHistory = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(history).save(savedHistory.capture());
        assertThat(savedHistory.getValue().getFromStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(savedHistory.getValue().getNote()).isEqualTo("Changed my mind");
        verify(vouchers).release("SALE10");
        verify(ledger).saveAll(any());
    }

    @Test
    void cancelMyOrder_whenAlreadyCancelled_rejectsWithoutReleasingAgain() {
        UUID orderId = UUID.randomUUID();
        Order order = pendingOrder("SALE10");
        order.changeStatus(OrderStatus.CANCELLED, USER_ID, null, Instant.now());
        when(orders.findByIdAndUserIdForUpdate(orderId, USER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancelMyOrder(USER_ID, orderId, null))
                .isInstanceOfSatisfying(ConflictException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_INVALID_STATUS_TRANSITION));
        verifyNoInteractions(vouchers, inventory, ledger);
    }

    @Test
    void cancelMyOrder_whenMissingOrOwnedByAnotherUser_returnsNotFound() {
        UUID orderId = UUID.randomUUID();
        when(orders.findByIdAndUserIdForUpdate(orderId, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelMyOrder(USER_ID, orderId, null))
                .isInstanceOfSatisfying(ResourceNotFoundException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND));
    }

    // ===== Admin =====

    private static final UUID ADMIN_ID = UUID.randomUUID();

    @Test
    void getOrders_withBlankOrderNumber_ignoresThatFilter() {
        when(orders.search(eq(OrderStatus.CONFIRMED), eq(USER_ID), eq(null), any())).thenReturn(Page.empty());

        service.getOrders(OrderStatus.CONFIRMED, USER_ID, "  ", 0, 20);

        verify(orders).search(eq(OrderStatus.CONFIRMED), eq(USER_ID), eq(null), any());
    }

    @Test
    void updateOrderStatus_toConfirmed_onlyChangesStatus() {
        UUID orderId = UUID.randomUUID();
        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(pendingOrder("SALE10")));

        OrderResponse response = service.updateOrderStatus(ADMIN_ID, orderId, statusRequest(OrderStatus.CONFIRMED));

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(history).save(any());
        verifyNoInteractions(vouchers, inventory, ledger);
    }

    @Test
    void updateOrderStatus_toExpired_releasesVoucherAndStock() {
        UUID orderId = UUID.randomUUID();
        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(pendingOrder("SALE10")));
        when(inventory.release(eq(PRODUCT_ID), eq(2), any())).thenReturn(1);

        service.updateOrderStatus(ADMIN_ID, orderId, statusRequest(OrderStatus.EXPIRED));

        verify(vouchers).release("SALE10");
        verify(inventory).release(eq(PRODUCT_ID), eq(2), any());
    }

    @Test
    void updateOrderStatus_toShipping_takesReservedStockOutOfWarehouse() {
        UUID orderId = UUID.randomUUID();
        Order order = pendingOrder(null);
        order.changeStatus(OrderStatus.CONFIRMED, ADMIN_ID, null, Instant.now());
        order.changeStatus(OrderStatus.PROCESSING, ADMIN_ID, null, Instant.now());
        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(inventory.sell(eq(PRODUCT_ID), eq(2), any())).thenReturn(1);

        OrderResponse response = service.updateOrderStatus(ADMIN_ID, orderId, statusRequest(OrderStatus.SHIPPING));

        assertThat(response.getStatus()).isEqualTo(OrderStatus.SHIPPING);
        verify(inventory).sell(eq(PRODUCT_ID), eq(2), any());
        verify(ledger).saveAll(any());
    }

    @Test
    void updateOrderStatus_whenTransitionInvalid_rejectsWithoutTouchingStock() {
        UUID orderId = UUID.randomUUID();
        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(pendingOrder(null)));

        assertThatThrownBy(() -> service.updateOrderStatus(ADMIN_ID, orderId, statusRequest(OrderStatus.SHIPPING)))
                .isInstanceOfSatisfying(ConflictException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_INVALID_STATUS_TRANSITION));
        verifyNoInteractions(inventory, ledger);
    }

    private UpdateOrderStatusRequest statusRequest(OrderStatus status) {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(status);
        return request;
    }

    /** Đơn PENDING_PAYMENT gồm 2 x 20.00 của PRODUCT_ID. */
    private Order pendingOrder(String voucherCode) {
        Instant now = Instant.now();
        return Order.create(
                USER_ID, "SC-20261126-ABC123",
                new Order.ShippingAddress("Nguyen Van An", "0901234567", "12 Nguyen Trai", "Ward 1", "District 5", "HCMC"),
                List.of(OrderItem.create(PRODUCT_ID, "SKU-001", "Checkout product", new BigDecimal("20.00"), 2)),
                voucherCode, BigDecimal.ZERO, KEY, "a".repeat(64), now, now.plus(Duration.ofMinutes(15))
        );
    }

    private void stubCheckout() {
        Product product = Product.builder().sku("SKU-001").name("Checkout product").price(new BigDecimal("20.00")).build();
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        when(users.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(User.builder().build()));
        when(carts.findByUserId(USER_ID)).thenAnswer(invocation -> {
            Cart cart = Cart.create(USER_ID);
            cart.addItem(product, 2);
            return Optional.of(cart);
        });
        CheckoutProduct snapshot = mock(CheckoutProduct.class);
        when(snapshot.getId()).thenReturn(PRODUCT_ID);
        when(snapshot.getSku()).thenReturn(product.getSku());
        when(snapshot.getName()).thenReturn(product.getName());
        when(snapshot.getPrice()).thenReturn(product.getPrice());
        when(products.findCheckoutProducts(List.of(PRODUCT_ID), ProductStatus.ACTIVE)).thenReturn(List.of(snapshot));
    }

    private CreateOrderRequest request() {
        ShippingAddressRequest address = new ShippingAddressRequest();
        address.setRecipientName("Nguyen Van An");
        address.setRecipientPhone("0901234567");
        address.setAddressLine("12 Nguyen Trai");
        address.setWard("Ward 1");
        address.setDistrict("District 5");
        address.setCity("Ho Chi Minh City");
        CreateOrderRequest request = new CreateOrderRequest();
        request.setShippingAddress(address);
        return request;
    }
}
