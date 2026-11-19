package io.github.datnguyen2810.shopcore.inventory;

import io.github.datnguyen2810.shopcore.inventory.dto.InventoryResponse;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {
    public InventoryResponse toResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .productId(inventory.getProductId())
                .availableQuantity(inventory.getAvailableQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .version(inventory.getVersion())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }


}
