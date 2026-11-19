package io.github.datnguyen2810.shopcore.inventory;

import io.github.datnguyen2810.shopcore.common.exception.ConflictException;
import io.github.datnguyen2810.shopcore.common.exception.ErrorCode;
import io.github.datnguyen2810.shopcore.common.exception.InvalidRequestException;
import io.github.datnguyen2810.shopcore.inventory.dto.ImportInventoryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryMapper inventoryMapper;
    @Mock private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock private Inventory inventory;
    @InjectMocks private InventoryServiceImpl inventoryService;

    private final UUID productId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final ImportInventoryRequest request = new ImportInventoryRequest();

    @BeforeEach
    void setUp() {
        request.setQuantity(1);
        request.setVersion(0L);
        when(inventoryRepository.findByProductIdAndProductNotDeleted(productId)).thenReturn(Optional.of(inventory));
        when(inventory.getVersion()).thenReturn(0L);
    }

    @Test
    void importInventory_whenConcurrentUpdateWins_reportsVersionConflict() {
        doThrow(new OptimisticLockingFailureException("Concurrent update"))
                .when(inventoryRepository).flush();

        assertThatExceptionOfType(ConflictException.class)
                .isThrownBy(() -> inventoryService.importInventory(productId, userId, request))
                .satisfies(error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVENTORY_VERSION_CONFLICT));
        verifyNoInteractions(inventoryTransactionRepository);
    }

    @Test
    void importInventory_whenQuantityWouldOverflow_reportsInvalidRequest() {
        when(inventory.getAvailableQuantity()).thenReturn(Integer.MAX_VALUE);

        assertThatExceptionOfType(InvalidRequestException.class)
                .isThrownBy(() -> inventoryService.importInventory(productId, userId, request))
                .satisfies(error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVENTORY_QUANTITY_OVERFLOW));
        verifyNoInteractions(inventoryTransactionRepository);
    }
}
