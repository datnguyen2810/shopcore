package io.github.datnguyen2810.shopcore.inventory;

import io.github.datnguyen2810.shopcore.common.exception.ConflictException;
import io.github.datnguyen2810.shopcore.common.exception.ErrorCode;
import io.github.datnguyen2810.shopcore.common.exception.InvalidRequestException;
import io.github.datnguyen2810.shopcore.common.exception.ResourceNotFoundException;
import io.github.datnguyen2810.shopcore.common.response.PageResponse;
import io.github.datnguyen2810.shopcore.inventory.dto.AdjustInventoryRequest;
import io.github.datnguyen2810.shopcore.inventory.dto.ImportInventoryRequest;
import io.github.datnguyen2810.shopcore.inventory.dto.InventoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService{
    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public PageResponse<InventoryResponse> getInventories(int currentPage, int pageSize) {
        Pageable pageable = PageRequest.of(
                currentPage,
                pageSize,
                Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.asc("productId"))
        );

        Page<InventoryResponse> result = inventoryRepository.findAllByProductNotDeleted(pageable)
                .map(inventoryMapper::toResponse);

        return PageResponse.from(result);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public InventoryResponse getInventoryById(UUID productId) {
        Inventory inventory = inventoryRepository.findByProductIdAndProductNotDeleted(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.INVENTORY_NOT_FOUND,
                        "Inventory not found for product: " + productId
                ));

        return inventoryMapper.toResponse(inventory);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    public InventoryResponse importInventory(UUID productId, UUID userId, ImportInventoryRequest request) {
        Inventory inventory = inventoryRepository.findByProductIdAndProductNotDeleted(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.INVENTORY_NOT_FOUND,
                        "Inventory not found for product: " + productId
                ));

        // Xử lý Optimistic Locking
        if (!Objects.equals(inventory.getVersion(), request.getVersion())) {
            throw new ConflictException(
                    ErrorCode.INVENTORY_VERSION_CONFLICT,
                    "Inventory has been modified by another request"
            );
        }

        int quantity = request.getQuantity();
        if ((long) inventory.getAvailableQuantity() + quantity > Integer.MAX_VALUE) {
            throw new InvalidRequestException(
                    ErrorCode.INVENTORY_QUANTITY_OVERFLOW,
                    ErrorCode.INVENTORY_QUANTITY_OVERFLOW.getDefaultMessage()
            );
        }

        inventory.importStock(quantity);

        // Flush để Hibernate cập nhật tồn kho và kiểm tra version ngay trong try/catch,
        // nhờ đó có thể trả 409 nếu request khác đã cập nhật trước.
        try {
            inventoryRepository.flush(); // chỉ cần flush vì inventory đang được managed
        } catch (OptimisticLockingFailureException exception) {
            throw new ConflictException(
                    ErrorCode.INVENTORY_VERSION_CONFLICT,
                    ErrorCode.INVENTORY_VERSION_CONFLICT.getDefaultMessage(),
                    exception
            );
        }

        inventoryTransactionRepository.save(InventoryTransaction.create(
                productId, null, userId, InventoryTransactionType.IMPORT, quantity, request.getNote()
        ));

        return inventoryMapper.toResponse(inventory);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    public InventoryResponse adjustInventory(UUID productId, UUID userId, AdjustInventoryRequest request) {
        Inventory inventory = inventoryRepository.findByProductIdAndProductNotDeleted(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.INVENTORY_NOT_FOUND,
                        "Inventory not found for product: " + productId
                ));

        // Xử lý Optimistic Locking
        if (!Objects.equals(inventory.getVersion(), request.getVersion())) {
            throw new ConflictException(
                    ErrorCode.INVENTORY_VERSION_CONFLICT,
                    "Inventory has been modified by another request"
            );
        }

        int delta = request.getQuantityDelta();
        long newQuantity = (long) inventory.getAvailableQuantity() + delta;
        if (newQuantity > Integer.MAX_VALUE) {
            throw new InvalidRequestException(
                    ErrorCode.INVENTORY_QUANTITY_OVERFLOW,
                    ErrorCode.INVENTORY_QUANTITY_OVERFLOW.getDefaultMessage()
            );
        }
        if (newQuantity < 0) {
            throw new InvalidRequestException(
                    ErrorCode.INVENTORY_QUANTITY_UNDERFLOW,
                    ErrorCode.INVENTORY_QUANTITY_UNDERFLOW.getDefaultMessage()
            );
        }

        inventory.adjustAvailableQuantity(delta);

        try {
            inventoryRepository.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw new ConflictException(
                    ErrorCode.INVENTORY_VERSION_CONFLICT,
                    ErrorCode.INVENTORY_VERSION_CONFLICT.getDefaultMessage(),
                    exception
            );
        }

        inventoryTransactionRepository.save(InventoryTransaction.create(
                productId, null, userId, InventoryTransactionType.ADJUST, delta, request.getNote()
        ));

        return inventoryMapper.toResponse(inventory);
    }

}
