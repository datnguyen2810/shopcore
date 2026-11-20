package io.github.datnguyen2810.shopcore.cart;

import io.github.datnguyen2810.shopcore.product.Product;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    public static Cart create(UUID userId) {
        Cart cart = new Cart();
        cart.userId = Objects.requireNonNull(userId, "User ID is required");
        return cart;
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public CartItem addItem(Product product, int quantity) {
        CartItem item = CartItem.create(this, product, quantity);
        items.add(item);
        return item;
    }

    public boolean removeItem(UUID itemId) {
        return items.removeIf(item -> itemId.equals(item.getId()));
    }

    public void clearItems() {
        items.clear();
    }
}
