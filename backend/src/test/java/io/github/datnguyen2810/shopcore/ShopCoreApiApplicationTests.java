package io.github.datnguyen2810.shopcore;

import io.github.datnguyen2810.shopcore.category.Category;
import io.github.datnguyen2810.shopcore.category.CategoryRepository;
import io.github.datnguyen2810.shopcore.common.response.PageResponse;
import io.github.datnguyen2810.shopcore.inventory.Inventory;
import io.github.datnguyen2810.shopcore.inventory.InventoryRepository;
import io.github.datnguyen2810.shopcore.inventory.InventoryService;
import io.github.datnguyen2810.shopcore.inventory.dto.InventoryResponse;
import io.github.datnguyen2810.shopcore.product.ProductService;
import io.github.datnguyen2810.shopcore.product.dto.CreateProductRequest;
import io.github.datnguyen2810.shopcore.product.dto.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ShopCoreApiApplicationTests {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductService productService;

    @Test
    void contextLoads() {
    }

    @Test
    @WithMockUser(authorities = "CATALOG_MANAGE")
    void createProduct_persistsInventoryWithZeroStock() {
        Category category = categoryRepository.save(new Category("Accessories", "accessories", null));
        CreateProductRequest request = new CreateProductRequest();
        request.setCategoryId(category.getId());
        request.setSku("CASE-001");
        request.setName("Phone Case");
        request.setSlug("phone-case");
        request.setPrice(new BigDecimal("10.00"));

        ProductResponse product = productService.createProduct(request);

        Inventory inventory = inventoryRepository.findById(product.getId()).orElseThrow();
        assertThat(inventory.getAvailableQuantity()).isZero();
        assertThat(inventory.getReservedQuantity()).isZero();
        assertThat(inventory.getVersion()).isZero();
    }

    @Test
    @Transactional
    @WithMockUser(authorities = {"CATALOG_MANAGE", "INVENTORY_READ"})
    void getInventories_excludesSoftDeletedProductsFromItemsAndTotals() {
        Category category = categoryRepository.save(new Category("Inventory", "inventory", null));
        CreateProductRequest request = new CreateProductRequest();
        request.setCategoryId(category.getId());
        request.setSku("INVENTORY-KEEP");
        request.setName("Product to keep");
        request.setSlug("inventory-keep");
        request.setPrice(BigDecimal.TEN);
        ProductResponse keptProduct = productService.createProduct(request);

        request.setSku("INVENTORY-DELETE");
        request.setName("Product to delete");
        request.setSlug("inventory-delete");
        ProductResponse deletedProduct = productService.createProduct(request);

        long totalBeforeDeletion = inventoryService.getInventories(0, 1).getTotalElements();
        productService.deleteById(deletedProduct.getId());

        PageResponse<InventoryResponse> page = inventoryService.getInventories(0, 1);

        assertThat(page.getItems()).extracting(InventoryResponse::getProductId)
                .containsExactly(keptProduct.getId());
        assertThat(page.getTotalElements()).isEqualTo(totalBeforeDeletion - 1);
        assertThat(page.getTotalPages()).isEqualTo(totalBeforeDeletion - 1);
        assertThat(inventoryRepository.existsById(deletedProduct.getId())).isTrue();
    }
}
