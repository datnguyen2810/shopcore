package io.github.datnguyen2810.shopcore.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "inventory_transactions") // lưu lịch sử biến động kho
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "order_id", updatable = false)
    private UUID orderId;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, length = 30)
    private InventoryTransactionType type;

    @Column(name = "quantity", nullable = false, updatable = false)
    private int quantity;

    @Column(name = "note", updatable = false, length = 500)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static InventoryTransaction create(
            UUID productId,
            UUID orderId,
            UUID createdBy,
            InventoryTransactionType type,
            int quantity,
            String note
    ) {
        Objects.requireNonNull(productId, "Product ID is required");
        Objects.requireNonNull(type, "Transaction type is required");

        if (type == InventoryTransactionType.ADJUST) {
            if (quantity == 0) {
                throw new IllegalArgumentException("Adjustment quantity must not be zero");
            }
            if (note == null || note.isBlank()) {
                throw new IllegalArgumentException("Adjustment note is required");
            }
        } else if (quantity <= 0) {
            throw new IllegalArgumentException("Transaction quantity must be positive");
        }

        if (note != null && note.length() > 500) {
            throw new IllegalArgumentException("Note must not exceed 500 characters");
        }

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.productId = productId;
        transaction.orderId = orderId;
        transaction.createdBy = createdBy;
        transaction.type = type;
        transaction.quantity = quantity;
        transaction.note = note == null || note.isBlank() ? null : note.strip();
        return transaction;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
