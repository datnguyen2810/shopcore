package io.github.datnguyen2810.shopcore.cart;

import io.github.datnguyen2810.shopcore.cart.dto.AddCartItemRequest;
import io.github.datnguyen2810.shopcore.cart.dto.CartResponse;
import io.github.datnguyen2810.shopcore.cart.dto.UpdateCartItemRequest;
import jakarta.validation.Valid;

import java.util.UUID;

public interface CartService {
    CartResponse getCart(UUID userId);

    CartResponse addItem(UUID userId, @Valid AddCartItemRequest request);

    CartResponse updateItemQuantity(UUID userId, UUID itemId, @Valid UpdateCartItemRequest request);

    void deleteItemFromCart(UUID userId, UUID itemId);

    void clearCart(UUID userId);
}
