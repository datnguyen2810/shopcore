package io.github.datnguyen2810.shopcore.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "inventories")
public class Inventory {

    @Id
    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Inventory create(UUID productId) {
        Inventory inventory = new Inventory();
        inventory.productId = Objects.requireNonNull(productId, "Product ID is required");
        return inventory;
    }

    public void importStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Import quantity must be positive");
        }
        adjustAvailableQuantity(quantity);
    }

    public void adjustAvailableQuantity(int quantityDelta) {
        if (quantityDelta == 0) {
            throw new IllegalArgumentException("Quantity delta must not be zero");
        }

        long newQuantity = (long) availableQuantity + quantityDelta;
        if (newQuantity < 0 || newQuantity > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Available quantity is out of range");
        }
        availableQuantity = (int) newQuantity;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
