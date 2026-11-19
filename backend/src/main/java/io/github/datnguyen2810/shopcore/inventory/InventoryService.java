package io.github.datnguyen2810.shopcore.inventory;

import io.github.datnguyen2810.shopcore.common.response.PageResponse;
import io.github.datnguyen2810.shopcore.inventory.dto.AdjustInventoryRequest;
import io.github.datnguyen2810.shopcore.inventory.dto.ImportInventoryRequest;
import io.github.datnguyen2810.shopcore.inventory.dto.InventoryResponse;
import jakarta.validation.Valid;

import java.util.UUID;

public interface InventoryService {

    PageResponse<InventoryResponse> getInventories(int currentPage, int pageSize);

    InventoryResponse getInventoryById(UUID productId);

    InventoryResponse importInventory(UUID productId, UUID userId, @Valid ImportInventoryRequest request);

    InventoryResponse adjustInventory(UUID productId, UUID userId, @Valid AdjustInventoryRequest request);
}
