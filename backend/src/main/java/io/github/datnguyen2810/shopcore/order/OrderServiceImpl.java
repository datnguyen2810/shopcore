package io.github.datnguyen2810.shopcore.order;

import io.github.datnguyen2810.shopcore.cart.Cart;
import io.github.datnguyen2810.shopcore.cart.CartRepository;
import io.github.datnguyen2810.shopcore.common.exception.ConflictException;
import io.github.datnguyen2810.shopcore.common.exception.ErrorCode;
import io.github.datnguyen2810.shopcore.common.exception.InvalidRequestException;
import io.github.datnguyen2810.shopcore.common.exception.ResourceNotFoundException;
import io.github.datnguyen2810.shopcore.inventory.InventoryRepository;
import io.github.datnguyen2810.shopcore.inventory.InventoryTransaction;
import io.github.datnguyen2810.shopcore.inventory.InventoryTransactionRepository;
import io.github.datnguyen2810.shopcore.inventory.InventoryTransactionType;
import io.github.datnguyen2810.shopcore.order.dto.CreateOrderRequest;
import io.github.datnguyen2810.shopcore.order.dto.OrderResponse;
import io.github.datnguyen2810.shopcore.product.ProductRepository;
import io.github.datnguyen2810.shopcore.product.ProductRepository.CheckoutProduct;
import io.github.datnguyen2810.shopcore.product.ProductStatus;
import io.github.datnguyen2810.shopcore.user.User;
import io.github.datnguyen2810.shopcore.user.UserRepository;
import io.github.datnguyen2810.shopcore.user.UserStatus;
import io.github.datnguyen2810.shopcore.voucher.Voucher;
import io.github.datnguyen2810.shopcore.voucher.VoucherCalculator;
import io.github.datnguyen2810.shopcore.voucher.VoucherRepository;
import io.github.datnguyen2810.shopcore.common.response.PageResponse;
import io.github.datnguyen2810.shopcore.order.dto.OrderSummaryResponse;
import io.github.datnguyen2810.shopcore.order.dto.UpdateOrderStatusRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(OrderProperties.class)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final VoucherRepository voucherRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final VoucherCalculator voucherCalculator;
    private final OrderNumberGenerator orderNumberGenerator;
    private final OrderMapper orderMapper;
    private final OrderProperties orderProperties;
    private final JsonMapper jsonMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getMyOrders(UUID userId, OrderStatus status, int currentPage, int pageSize) {
        Pageable pageable = newestFirst(currentPage, pageSize);
        Page<Order> orders = status == null
                ? orderRepository.findByUserId(userId, pageable)
                : orderRepository.findByUserIdAndStatus(userId, status, pageable);
        return PageResponse.from(orders.map(orderMapper::toOrderSummaryResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(UUID userId, UUID orderId) {
        // Đơn của người khác cũng trả 404 (không phải 403) để không lộ việc đơn đó tồn tại.
        return orderRepository.findByIdAndUserId(orderId, userId)
                .map(orderMapper::toOrderResponse)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND));
    }

    @Override
    @Transactional
    public OrderResponse cancelMyOrder(UUID userId, UUID orderId, String reason) {
        // Lock đơn: 2 lần huỷ đồng thời -> lần sau chờ, rồi thấy CANCELLED và nhận 409
        Order order = orderRepository.findByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND));
        return changeStatus(order, OrderStatus.CANCELLED, userId, reason);
    }

    // ===== Admin =====

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ORDER_READ_ALL')")
    public PageResponse<OrderSummaryResponse> getOrders(OrderStatus status, UUID userId, String orderNumber,
                                                        int currentPage, int pageSize) {
        String normalizedOrderNumber = orderNumber == null || orderNumber.isBlank() ? null : orderNumber.strip();
        Page<Order> orders = orderRepository.search(status, userId, normalizedOrderNumber, newestFirst(currentPage, pageSize));
        return PageResponse.from(orders.map(orderMapper::toOrderSummaryResponse));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ORDER_READ_ALL')")
    public OrderResponse getOrder(UUID orderId) {
        return orderRepository.findWithItemsById(orderId)
                .map(orderMapper::toOrderResponse)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ORDER_UPDATE')")
    public OrderResponse updateOrderStatus(UUID adminId, UUID orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND));
        return changeStatus(order, request.getStatus(), adminId, request.getNote());
    }

    // ===== Scheduler =====

    @Override
    @Transactional
    public boolean expireOrder(UUID orderId, Instant now) {
        // Kiểm tra lại sau khi lock: lúc chờ lock, khách có thể vừa huỷ (hoặc từ M6: vừa thanh toán)
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING_PAYMENT
                || order.getPaymentExpiresAt().isAfter(now)) {
            return false;
        }
        changeStatus(order, OrderStatus.EXPIRED, null, "Payment timeout"); // actor null = hệ thống
        return true;
    }

    /**
     * Đổi trạng thái đơn (đã được lock) + ghi lịch sử + xử lý kho/voucher đi kèm.
     * OrderStatePolicy chặn chuyển trạng thái sai (409), nên CANCELLED/EXPIRED chỉ đến được từ PENDING_PAYMENT.
     */
    private OrderResponse changeStatus(Order order, OrderStatus target, UUID actorId, String note) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        orderStatusHistoryRepository.save(order.changeStatus(target, actorId, note, now));

        switch (target) {
            case CANCELLED, EXPIRED -> {
                // Hoàn voucher rồi trả hàng về kho (cùng thứ tự lock voucher -> inventory như checkout)
                if (order.getVoucherCode() != null) {
                    voucherRepository.release(order.getVoucherCode());
                }
                moveStock(order, InventoryTransactionType.RELEASE, actorId, now);
            }
            case SHIPPING -> moveStock(order, InventoryTransactionType.SALE, actorId, now); // hàng rời kho
            default -> { } // CONFIRMED, PROCESSING, COMPLETED: chỉ đổi trạng thái
        }

        orderRepository.flush(); // để response trả version mới sau khi đổi trạng thái
        return orderMapper.toOrderResponse(order);
    }

    /** createdAt DESC khớp index (user_id, created_at DESC); id phân xử khi trùng createdAt để trang không lệch. */
    private Pageable newestFirst(int currentPage, int pageSize) {
        return PageRequest.of(currentPage, pageSize, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    @Override
    @Transactional
    public OrderResponse createOrder(UUID userId, String idempotencyKey, CreateOrderRequest request) {
        // 1. Chuẩn hoá input
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new InvalidRequestException(ErrorCode.ORDER_INVALID_IDEMPOTENCY_KEY);
        }
        Order.ShippingAddress address = orderMapper.toShippingAddress(request.getShippingAddress());
        String voucherCode = request.getVoucherCode() == null || request.getVoucherCode().isBlank()
                ? null : request.getVoucherCode().strip();
        String requestHash = hashRequest(address, voucherCode);

        // 2. Lock user (cart writes lấy cùng lock này), rồi replay nếu key đã được dùng
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("User is not active");
        }
        Optional<Order> existing = orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(requestHash)) {
                throw new ConflictException(ErrorCode.ORDER_IDEMPOTENCY_CONFLICT);
            }
            return orderMapper.toOrderResponse(existing.get());
        }

        // 3. Tính tiền từ giỏ hàng và voucher
        Cart cart = cartRepository.findByUserId(userId)
                .filter(value -> !value.getItems().isEmpty())
                .orElseThrow(() -> new InvalidRequestException(ErrorCode.ORDER_CART_EMPTY));
        List<OrderItem> items;
        BigDecimal subtotal;
        try {
            items = createOrderItems(cart);
            subtotal = Order.requireAmount(
                    items.stream().map(OrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add), "Subtotal");
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException(ErrorCode.ORDER_AMOUNT_OUT_OF_RANGE);
        }
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Voucher voucher = voucherCode == null ? null : voucherRepository.findByCode(voucherCode)
                .filter(value -> value.isUsableFor(subtotal, now))
                .orElseThrow(() -> new ConflictException(ErrorCode.ORDER_VOUCHER_UNAVAILABLE));
        BigDecimal discount = voucher == null ? BigDecimal.ZERO : voucherCalculator.calculateDiscount(voucher, subtotal);

        // 4. Lưu đơn và xoá giỏ hàng
        Order order = Order.create(
                userId, orderNumberGenerator.generate(now), address, items, voucherCode, discount,
                idempotencyKey, requestHash, now, now.plus(orderProperties.paymentTimeout())
        );
        orderRepository.save(order);
        orderStatusHistoryRepository.save(OrderStatusHistory.created(order));
        cart.clearItems();
        // Phát hiện vi phạm constraint của order trước khi lock các dòng voucher/inventory dùng chung.
        orderRepository.flush();

        // 5. Trừ lượt voucher (UPDATE có điều kiện, trả 0 nếu vừa hết lượt/bị sửa) và giữ hàng trong kho
        if (voucher != null && voucherRepository.consume(
                voucher.getId(), now, subtotal, voucher.getDiscountType().name(),
                voucher.getDiscountValue(), voucher.getMaximumDiscount()
        ) == 0) {
            throw new ConflictException(ErrorCode.ORDER_VOUCHER_UNAVAILABLE);
        }
        moveStock(order, InventoryTransactionType.RESERVE, userId, now);
        return orderMapper.toOrderResponse(order);
    }

    private List<OrderItem> createOrderItems(Cart cart) {
        List<UUID> productIds = cart.getItems().stream().map(item -> item.getProduct().getId()).toList();
        Map<UUID, CheckoutProduct> products = productRepository.findCheckoutProducts(productIds, ProductStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(CheckoutProduct::getId, Function.identity()));

        return cart.getItems().stream().map(item -> {
            CheckoutProduct product = products.get(item.getProduct().getId());
            if (product == null) {
                throw new ConflictException(ErrorCode.ORDER_PRODUCT_UNAVAILABLE);
            }
            return OrderItem.create(
                    product.getId(), product.getSku(), product.getName(), product.getPrice(), item.getQuantity()
            );
        }).toList();
    }

    /**
     * Cập nhật kho cho mọi item của đơn và ghi sổ kho:
     * RESERVE (available -> reserved), RELEASE (reserved -> available), SALE (reserved -> rời kho).
     * Luôn theo thứ tự productId để các transaction đồng thời lock inventory cùng một thứ tự (tránh deadlock).
     */
    private void moveStock(Order order, InventoryTransactionType type, UUID actorId, Instant now) {
        List<OrderItem> items = order.getItems().stream().sorted(Comparator.comparing(OrderItem::getProductId)).toList();
        for (OrderItem item : items) {
            // UPDATE có điều kiện đã tự lấy row lock trên dòng inventory
            int updated = switch (type) {
                case RESERVE -> inventoryRepository.reserve(item.getProductId(), item.getQuantity(), now);
                case RELEASE -> inventoryRepository.release(item.getProductId(), item.getQuantity(), now);
                case SALE -> inventoryRepository.sell(item.getProductId(), item.getQuantity(), now);
                default -> throw new IllegalArgumentException("Unsupported stock movement: " + type);
            };
            if (updated == 0 && type == InventoryTransactionType.RESERVE) {
                ErrorCode code = ErrorCode.ORDER_INSUFFICIENT_STOCK;
                throw new ConflictException(code, code.getDefaultMessage() + " (product " + item.getProductId() + ")");
            }
            if (updated == 0) {
                // reserved < quantity nghĩa là dữ liệu kho đã lệch -> rollback, không âm thầm bỏ qua
                throw new IllegalStateException("Reserved stock is inconsistent for product " + item.getProductId());
            }
        }
        inventoryTransactionRepository.saveAll(items.stream()
                .map(item -> InventoryTransaction.create(
                        item.getProductId(), order.getId(), actorId, type, item.getQuantity(), null))
                .toList());
    }

    private String hashRequest(Order.ShippingAddress address, String voucherCode) {
        String[] fields = {
                address.getRecipientName(), address.getRecipientPhone(), address.getAddressLine(),
                address.getWard(), address.getDistrict(), address.getCity(), voucherCode
        };
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(jsonMapper.writeValueAsBytes(fields));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
