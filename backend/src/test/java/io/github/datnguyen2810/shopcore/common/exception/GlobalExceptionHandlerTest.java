package io.github.datnguyen2810.shopcore.common.exception;

import io.github.datnguyen2810.shopcore.inventory.AdminInventoryController;
import io.github.datnguyen2810.shopcore.inventory.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    @Test
    void adjustInventory_whenProductIdIsNotUuid_returnsBadRequest() throws Exception {
        InventoryService inventoryService = mock(InventoryService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AdminInventoryController(inventoryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(post("/api/v1/admin/inventories/abc/adjustments"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.productId").value("Parameter has an invalid format"));

        verifyNoInteractions(inventoryService);
    }
}
