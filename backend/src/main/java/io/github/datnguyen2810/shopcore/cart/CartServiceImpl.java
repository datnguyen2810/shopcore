package io.github.datnguyen2810.shopcore.cart;

import io.github.datnguyen2810.shopcore.cart.dto.AddCartItemRequest;
import io.github.datnguyen2810.shopcore.cart.dto.CartResponse;
import io.github.datnguyen2810.shopcore.cart.dto.UpdateCartItemRequest;
import io.github.datnguyen2810.shopcore.common.exception.ErrorCode;
import io.github.datnguyen2810.shopcore.common.exception.InvalidRequestException;
import io.github.datnguyen2810.shopcore.common.exception.ResourceNotFoundException;
import io.github.datnguyen2810.shopcore.product.Product;
import io.github.datnguyen2810.shopcore.product.ProductRepository;
import io.github.datnguyen2810.shopcore.product.ProductStatus;
import io.github.datnguyen2810.shopcore.user.User;
import io.github.datnguyen2810.shopcore.user.UserRepository;
import io.github.datnguyen2810.shopcore.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService{
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId) {
        return cartRepository.findByUserId(userId)
                .map(cartMapper::toCartResponse)
                .orElseGet(() -> CartResponse.builder()
                        .items(List.of())
                        .subtotal(BigDecimal.ZERO)
                        .build());
    }

    @Override
    @Transactional
    public CartResponse addItem(UUID userId, AddCartItemRequest request) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.USER_NOT_FOUND,
                        "User not found with id: " + userId
                ));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("User is not active");
        }

        UUID productId = request.getProductId();
        Product product = productRepository.findByIdAndStatusAndDeletedAtIsNullAndCategory_ActiveTrue(productId, ProductStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.PRODUCT_NOT_FOUND,
                        "Product not found with id: " + productId
                ));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.create(userId))); // chưa có giỏ tạo giỏ mới, orElseGet chỉ save khi Optionl rỗng

        int quantityAdd = request.getQuantity();

        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            long newQuantity = (long) existingItem.getQuantity() + quantityAdd;

            if (newQuantity > Integer.MAX_VALUE) {
                ErrorCode code = ErrorCode.CART_ITEM_QUANTITY_OVERFLOW;
                throw new InvalidRequestException(code, code.getDefaultMessage());
            }

            existingItem.updateQuantity((int) newQuantity);
        }
        else {
            CartItem newItem = cart.addItem(product, quantityAdd);
            cartItemRepository.save(newItem);
        }

        return cartMapper.toCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(UUID userId, UUID itemId, UpdateCartItemRequest request) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.USER_NOT_FOUND,
                        ErrorCode.USER_NOT_FOUND.getDefaultMessage()
                ));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("User is not active");
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(this::cartItemNotFound);

        CartItem item = cart.getItems().stream()
                .filter(cartItem -> itemId.equals(cartItem.getId()))
                .findFirst()
                .orElseThrow(this::cartItemNotFound);

        int newQuantity = request.getQuantity();

        // chỉ ktra product khi tăng số lượng
        if (newQuantity > item.getQuantity()) {
            productRepository.findByIdAndStatusAndDeletedAtIsNullAndCategory_ActiveTrue(
                    item.getProduct().getId(),
                    ProductStatus.ACTIVE
            ).orElseThrow(() -> new ResourceNotFoundException(
                    ErrorCode.PRODUCT_NOT_FOUND,
                    ErrorCode.PRODUCT_NOT_FOUND.getDefaultMessage()
            ));
        }

        item.updateQuantity(newQuantity);

        return cartMapper.toCartResponse(cart);
    }

    @Override
    @Transactional
    public void deleteItemFromCart(UUID userId, UUID itemId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.USER_NOT_FOUND,
                        ErrorCode.USER_NOT_FOUND.getDefaultMessage()
                ));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("User is not active");
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(this::cartItemNotFound);

        if (!cart.removeItem(itemId)) {
            throw cartItemNotFound();
        }
    }

    @Override
    @Transactional
    public void clearCart(UUID userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.USER_NOT_FOUND,
                        ErrorCode.USER_NOT_FOUND.getDefaultMessage()
                ));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("User is not active");
        }

        cartRepository.findByUserId(userId)
            .ifPresent(Cart::clearItems);
    }

    private ResourceNotFoundException cartItemNotFound() {
        ErrorCode code = ErrorCode.CART_ITEM_NOT_FOUND;
        return new ResourceNotFoundException(code, code.getDefaultMessage());
    }


}
