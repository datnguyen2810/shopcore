package io.github.datnguyen2810.shopcore.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "order_number", nullable = false, updatable = false, length = 50)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "subtotal", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "total_amount", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "voucher_code", updatable = false, length = 80)
    private String voucherCode;

    @Embedded
    private ShippingAddress shippingAddress;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, updatable = false, length = 64)
    private String requestHash;

    @Column(name = "payment_expires_at", nullable = false, updatable = false)
    private Instant paymentExpiresAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST)
    private List<OrderItem> items = new ArrayList<>();

    public static Order create(
            UUID userId,
            String orderNumber,
            ShippingAddress shippingAddress,
            List<OrderItem> items,
            String voucherCode,
            BigDecimal discountAmount,
            String idempotencyKey,
            String requestHash,
            Instant now,
            Instant paymentExpiresAt
    ) {
        Objects.requireNonNull(now, "Creation time is required");
        Objects.requireNonNull(paymentExpiresAt, "Payment expiration time is required");
        if (!paymentExpiresAt.isAfter(now)) {
            throw new IllegalArgumentException("Payment expiration time must be after order creation");
        }
        if (requestHash == null || !requestHash.matches("[0-9A-Fa-f]{64}")) {
            throw new IllegalArgumentException("Request hash must be a SHA-256 hex digest");
        }

        List<OrderItem> snapshots = List.copyOf(items);
        if (snapshots.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        Set<UUID> productIds = new HashSet<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : snapshots) {
            if (item.getOrder() != null) {
                throw new IllegalArgumentException("Item already belongs to an order");
            }
            if (!productIds.add(item.getProductId())) {
                throw new IllegalArgumentException("Product must appear only once per order");
            }
            subtotal = subtotal.add(item.getLineTotal());
        }

        Order order = new Order();
        order.userId = Objects.requireNonNull(userId, "User ID is required");
        order.orderNumber = requireText(orderNumber, "Order number", 50);
        order.status = OrderStatus.PENDING_PAYMENT;
        order.shippingAddress = Objects.requireNonNull(shippingAddress, "Shipping address is required");
        order.voucherCode = voucherCode == null ? null : requireText(voucherCode, "Voucher code", 80);
        order.subtotal = requireAmount(subtotal, "Subtotal");
        order.discountAmount = requireAmount(discountAmount, "Discount amount");
        if (order.discountAmount.compareTo(order.subtotal) > 0) {
            throw new IllegalArgumentException("Discount amount must not exceed subtotal");
        }
        order.totalAmount = order.subtotal.subtract(order.discountAmount);
        order.idempotencyKey = requireText(idempotencyKey, "Idempotency key", 100);
        order.requestHash = requestHash;
        order.paymentExpiresAt = paymentExpiresAt;
        order.createdAt = now;
        order.updatedAt = now;

        snapshots.forEach(item -> item.attachTo(order));
        order.items.addAll(snapshots);
        return order;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public OrderStatusHistory changeStatus(OrderStatus targetStatus, UUID changedBy, String note, Instant now) {
        OrderStatePolicy.validateTransition(status, targetStatus);
        OrderStatusHistory history = OrderStatusHistory.create(this, status, targetStatus, changedBy, note, now);
        status = targetStatus;
        updatedAt = now;
        return history;
    }

    static String requireText(String text, String field, int maxLength) {
        if (text == null || text.isBlank() || text.length() > maxLength) {
            throw new IllegalArgumentException(field + " must contain between 1 and " + maxLength + " characters");
        }
        return text;
    }

    static BigDecimal requireAmount(BigDecimal amount, String field) {
        BigDecimal normalized = Objects.requireNonNull(amount, field + " is required").stripTrailingZeros();
        if (normalized.signum() < 0 || normalized.scale() > 2
                || (long) normalized.precision() - normalized.scale() > 17) {
            throw new IllegalArgumentException(field + " must be non-negative and fit NUMERIC(19, 2)");
        }
        return normalized.setScale(2);
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Embeddable
    public static class ShippingAddress {

        @Column(name = "recipient_name", nullable = false, updatable = false, length = 150)
        private String recipientName;

        @Column(name = "recipient_phone", nullable = false, updatable = false, length = 32)
        private String recipientPhone;

        @Column(name = "address_line", nullable = false, updatable = false, length = 255)
        private String addressLine;

        @Column(name = "ward", nullable = false, updatable = false, length = 120)
        private String ward;

        @Column(name = "district", nullable = false, updatable = false, length = 120)
        private String district;

        @Column(name = "city", nullable = false, updatable = false, length = 120)
        private String city;

        public ShippingAddress(
                String recipientName,
                String recipientPhone,
                String addressLine,
                String ward,
                String district,
                String city
        ) {
            this.recipientName = requireText(recipientName, "Recipient name", 150);
            this.recipientPhone = requireText(recipientPhone, "Recipient phone", 32);
            this.addressLine = requireText(addressLine, "Address line", 255);
            this.ward = requireText(ward, "Ward", 120);
            this.district = requireText(district, "District", 120);
            this.city = requireText(city, "City", 120);
        }
    }
}
