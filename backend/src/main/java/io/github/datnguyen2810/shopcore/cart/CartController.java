package io.github.datnguyen2810.shopcore.cart;

import io.github.datnguyen2810.shopcore.cart.dto.AddCartItemRequest;
import io.github.datnguyen2810.shopcore.cart.dto.CartResponse;
import io.github.datnguyen2810.shopcore.cart.dto.UpdateCartItemRequest;
import io.github.datnguyen2810.shopcore.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        CartResponse cart = cartService.getCart(userId);

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Cart retrieved successfully", cart));
    }

    // thêm item cho 1 giỏ hàng
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItemToCart(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddCartItemRequest request
    ){
        UUID userId = UUID.fromString(jwt.getSubject());
        CartResponse cart = cartService.addItem(userId, request);

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Cart item added successfully", cart));
    }

    // change số lượng của 1 item trong cart của 1 user
    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItemQuantity(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ){
        UUID userId = UUID.fromString(jwt.getSubject());
        CartResponse cart = cartService.updateItemQuantity(userId, itemId, request);

        return  ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Cart item updated successfully", cart));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> deleteItemFromCart(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID itemId
    ){
        UUID userId = UUID.fromString(jwt.getSubject());
        cartService.deleteItemFromCart(userId, itemId);

        return ResponseEntity.ok(ApiResponse.message(HttpStatus.OK, "Cart item deleted successfully"));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(@AuthenticationPrincipal Jwt jwt){
        UUID userId = UUID.fromString(jwt.getSubject());
        cartService.clearCart(userId);

        return ResponseEntity.ok(ApiResponse.message(HttpStatus.OK, "Cart cleared successfully"));
    }
}
