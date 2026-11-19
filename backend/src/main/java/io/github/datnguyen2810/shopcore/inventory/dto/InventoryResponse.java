package io.github.datnguyen2810.shopcore.inventory.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class InventoryResponse {

    private final UUID productId;
    private final int availableQuantity;
    private final int reservedQuantity;
    private final Long version;
    private final Instant createdAt;
    private final Instant updatedAt;
}
