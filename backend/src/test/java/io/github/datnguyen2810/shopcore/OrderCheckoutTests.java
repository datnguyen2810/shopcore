package io.github.datnguyen2810.shopcore;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrderCheckoutTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void createOrder_withSameKeyAndRequest_returnsSavedOrderAfterClearingCart() throws Exception {
        UUID userId = createUser();
        UUID cartId = createCart(userId);
        UUID productId = createProduct(5);
        addCartItem(cartId, productId, 2);
        String voucherCode = createVoucher();
        String body = checkoutBody("12 Nguyen Trai", voucherCode);

        String originalResponse = checkout(userId, "checkout:first", body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.subtotal").value(40.0))
                .andExpect(jsonPath("$.data.discountAmount").value(5.0))
                .andExpect(jsonPath("$.data.totalAmount").value(35.0))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andReturn().getResponse().getContentAsString();

        jdbc.update("UPDATE products SET name = 'Updated product', price = 100 WHERE id = ?", productId);
        checkout(userId, "checkout:first", body)
                .andExpect(status().isCreated())
                .andExpect(content().json(originalResponse));
        checkout(userId, "checkout:first", checkoutBody("14 Nguyen Trai", voucherCode))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ORDER_IDEMPOTENCY_CONFLICT"));

        UUID orderId = jdbc.queryForObject("SELECT id FROM orders WHERE user_id = ?", UUID.class, userId);
        assertThat(count("SELECT count(*) FROM cart_items WHERE cart_id = ?", cartId)).isZero();
        assertThat(count("SELECT used_count FROM vouchers WHERE code = ?", voucherCode)).isEqualTo(1);
        assertThat(count("SELECT available_quantity FROM inventories WHERE product_id = ?", productId)).isEqualTo(3);
        assertThat(count("SELECT reserved_quantity FROM inventories WHERE product_id = ?", productId)).isEqualTo(2);
        assertThat(count("""
                SELECT count(*) FROM order_status_history
                WHERE order_id = ? AND from_status IS NULL AND to_status = 'PENDING_PAYMENT' AND changed_by = ?
                """, orderId, userId)).isEqualTo(1);
        assertThat(count("""
                SELECT count(*) FROM inventory_transactions
                WHERE order_id = ? AND type = 'RESERVE' AND quantity = 2 AND created_by = ?
                """, orderId, userId)).isEqualTo(1);
    }

    @Test
    void createOrder_whenLaterItemIsOutOfStock_rollsBackVoucherInventoryAndCart() throws Exception {
        UUID userId = createUser();
        UUID cartId = createCart(userId);
        UUID firstProduct = createProduct(0);
        UUID secondProduct = createProduct(0);
        UUID stockedProduct = firstProduct.compareTo(secondProduct) < 0 ? firstProduct : secondProduct;
        jdbc.update("UPDATE inventories SET available_quantity = 1 WHERE product_id = ?", stockedProduct);
        addCartItem(cartId, firstProduct, 1);
        addCartItem(cartId, secondProduct, 1);
        String voucherCode = createVoucher();

        checkout(userId, "checkout:rollback", checkoutBody("12 Nguyen Trai", voucherCode))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ORDER_INSUFFICIENT_STOCK"));

        assertThat(count("SELECT count(*) FROM orders WHERE user_id = ?", userId)).isZero();
        assertThat(count("SELECT count(*) FROM order_status_history WHERE changed_by = ?", userId)).isZero();
        assertThat(count("SELECT count(*) FROM inventory_transactions WHERE created_by = ?", userId)).isZero();
        assertThat(count("SELECT count(*) FROM cart_items WHERE cart_id = ?", cartId)).isEqualTo(2);
        assertThat(count("SELECT used_count FROM vouchers WHERE code = ?", voucherCode)).isZero();
        assertThat(count("SELECT available_quantity FROM inventories WHERE product_id = ?", stockedProduct)).isEqualTo(1);
        assertThat(count("SELECT reserved_quantity FROM inventories WHERE product_id = ?", stockedProduct)).isZero();
    }

    @Test
    void createOrder_whenTwoUsersCompeteForLastUnit_createsOneOrder() throws Exception {
        UUID firstUser = createUser();
        UUID secondUser = createUser();
        UUID productId = createProduct(1);
        addCartItem(createCart(firstUser), productId, 1);
        addCartItem(createCart(secondUser), productId, 1);
        String body = checkoutBody("12 Nguyen Trai", null);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                start.await();
                return checkout(firstUser, "checkout:last-unit", body).andReturn().getResponse().getStatus();
            });
            var second = executor.submit(() -> {
                start.await();
                return checkout(secondUser, "checkout:last-unit", body).andReturn().getResponse().getStatus();
            });
            start.countDown();

            assertThat(new Integer[] {first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)})
                    .containsExactlyInAnyOrder(201, 409);
        }

        assertThat(count("SELECT count(*) FROM orders WHERE user_id IN (?, ?)", firstUser, secondUser)).isEqualTo(1);
        assertThat(count("SELECT reserved_quantity FROM inventories WHERE product_id = ?", productId)).isEqualTo(1);
        assertThat(count("SELECT available_quantity FROM inventories WHERE product_id = ?", productId)).isZero();
    }

    @Test
    void createOrder_whenHeaderMissing_returnsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/orders")
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody("12 Nguyen Trai", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ORDER_INVALID_IDEMPOTENCY_KEY"));
    }

    private ResultActions checkout(UUID userId, String key, String body) throws Exception {
        return mvc.perform(post("/api/v1/orders")
                .with(jwt().jwt(token -> token.subject(userId.toString())))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String checkoutBody(String addressLine, String voucherCode) {
        String voucherJson = voucherCode == null ? "null" : "\"" + voucherCode + "\"";
        return """
                {
                  "shippingAddress": {
                    "recipientName": "Nguyen Van An",
                    "recipientPhone": "0901234567",
                    "addressLine": "%s",
                    "ward": "Ward 1",
                    "district": "District 5",
                    "city": "Ho Chi Minh City"
                  },
                  "voucherCode": %s
                }
                """.formatted(addressLine, voucherJson);
    }

    private UUID createUser() {
        UUID id = UUID.randomUUID();
        String username = "checkout_" + id.toString().replace("-", "");
        jdbc.update("""
                INSERT INTO users (id, email, username, password_hash, full_name)
                VALUES (?, ?, ?, 'test-password-hash', 'Checkout Test')
                """, id, username + "@example.com", username);
        return id;
    }

    private UUID createCart(UUID userId) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO carts (id, user_id) VALUES (?, ?)", id, userId);
        return id;
    }

    private UUID createProduct(int available) {
        UUID categoryId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO categories (id, name, slug) VALUES (?, 'Checkout', ?)", categoryId, categoryId.toString());
        jdbc.update("""
                INSERT INTO products (id, category_id, sku, name, slug, price, status)
                VALUES (?, ?, ?, 'Checkout product', ?, 20.00, 'ACTIVE')
                """, id, categoryId, id.toString(), id.toString());
        jdbc.update("INSERT INTO inventories (product_id, available_quantity) VALUES (?, ?)", id, available);
        return id;
    }

    private void addCartItem(UUID cartId, UUID productId, int quantity) {
        jdbc.update("INSERT INTO cart_items (id, cart_id, product_id, quantity) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), cartId, productId, quantity);
    }

    private String createVoucher() {
        String code = "WELCOME-" + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO vouchers (id, code, discount_type, discount_value, usage_limit, starts_at, expires_at)
                VALUES (?, ?, 'FIXED_AMOUNT', 5.00, 1, now() - interval '1 day', now() + interval '1 day')
                """, UUID.randomUUID(), code);
        return code;
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }
}
