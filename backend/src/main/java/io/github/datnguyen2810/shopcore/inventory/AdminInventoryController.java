package io.github.datnguyen2810.shopcore.inventory;

import io.github.datnguyen2810.shopcore.common.response.ApiResponse;
import io.github.datnguyen2810.shopcore.common.response.PageResponse;
import io.github.datnguyen2810.shopcore.inventory.dto.AdjustInventoryRequest;
import io.github.datnguyen2810.shopcore.inventory.dto.ImportInventoryRequest;
import io.github.datnguyen2810.shopcore.inventory.dto.InventoryResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/inventories")
public class AdminInventoryController {
    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<InventoryResponse>>> getInventories(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be greater than or equal to 0")
            int currentPage,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Page size must be greater than or equal to 1")
            @Max(value = 100, message = "Page size must not exceed 100")
            int pageSize
    ) {
        PageResponse<InventoryResponse> inventories = inventoryService.getInventories(currentPage, pageSize);

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Inventories retrieved successfully", inventories));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventoryById(@PathVariable UUID productId) {
        InventoryResponse inventory = inventoryService.getInventoryById(productId);

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Inventory retrieved successfully", inventory));
    }

    // Nhập kho
    @PostMapping("/{productId}/imports")
    public ResponseEntity<ApiResponse<InventoryResponse>> importInventory(
            @PathVariable UUID productId,
            @Valid @RequestBody ImportInventoryRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        InventoryResponse inventory = inventoryService.importInventory(productId, userId, request);

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Inventory imported successfully", inventory));
    }

    // Điều chỉnh tồn kho (kiểm kê sai, hàng hỏng, ...)
    @PostMapping("/{productId}/adjustments")
    public ResponseEntity<ApiResponse< InventoryResponse>> adjustInventory(
            @PathVariable UUID productId,
            @Valid @RequestBody AdjustInventoryRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        InventoryResponse inventory = inventoryService.adjustInventory(productId, userId, request);

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Inventory adjusted successfully", inventory));
    }


}
