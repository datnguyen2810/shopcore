package io.github.datnguyen2810.shopcore.inventory.dto;

import io.github.datnguyen2810.shopcore.inventory.InventoryTransactionType;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class InventoryTransactionResponse {

    private final UUID id;
    private final UUID productId;
    private final UUID orderId;
    private final UUID createdBy;
    private final InventoryTransactionType type;
    private final int quantity;
    private final String note;
    private final Instant createdAt;
}
