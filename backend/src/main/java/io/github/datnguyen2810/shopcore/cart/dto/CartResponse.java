package io.github.datnguyen2810.shopcore.cart.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
public class CartResponse {

    private final UUID id;
    private final List<CartItemResponse> items;
    private final BigDecimal subtotal;

    @Builder
    public CartResponse(UUID id, List<CartItemResponse> items, BigDecimal subtotal) {
        this.id = id;
        this.items = List.copyOf(items);
        this.subtotal = subtotal;
    }
}
