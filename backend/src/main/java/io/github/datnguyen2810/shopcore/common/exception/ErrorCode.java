package io.github.datnguyen2810.shopcore.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    CATEGORY_SLUG_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "Category slug already exists"
    ),
    CATEGORY_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Category not found"
    ),
    CATEGORY_IN_USE(
            HttpStatus.CONFLICT,
            "Category cannot be deleted because it contains products"
    ),
    PRODUCT_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "Product SKU or slug already exists"
    ),
    PRODUCT_SKU_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "Product sku already exists"
    ),
    PRODUCT_SLUG_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "Product slug already exists"
    ),
    PRODUCT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Product not found"
    ),
    PRODUCT_VERSION_CONFLICT(
            HttpStatus.CONFLICT,
            "Product has been modified by another request"
    ),
    INVALID_PRICE_RANGE(
            HttpStatus.BAD_REQUEST,
            "Minimum price must not be greater than maximum price"
    ),
    INVALID_SORT_PARAMETER(
            HttpStatus.BAD_REQUEST,
            "Sort parameter is invalid"
    ),
    USERNAME_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "Username already exists"
    ),
    USER_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "Username or email already exists"
    ),
    EMAIL_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "Email already exists"
    ),
    INVALID_CREDENTIALS(
            HttpStatus.UNAUTHORIZED,
            "Invalid username or password"
    ),
    UNAUTHORIZED(
            HttpStatus.UNAUTHORIZED,
            "Authentication is required or token is invalid"
    ),
    FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "You do not have permission to access this resource"
    ),
    USER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "User not found"
    ),
    INVALID_REFRESH_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "Refresh token is invalid or expired"
    ),
    INVENTORY_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Inventory not found"
    ),
    INVENTORY_VERSION_CONFLICT(
            HttpStatus.CONFLICT,
            "Inventory has been modified by another request"
    ),
    INVENTORY_QUANTITY_OVERFLOW(
            HttpStatus.BAD_REQUEST,
            "Available quantity would exceed the maximum supported value"
    ),
    INVENTORY_QUANTITY_UNDERFLOW(
            HttpStatus.BAD_REQUEST,
            "Available quantity cannot be less than zero"
    ),
    CART_ITEM_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Cart item not found"
    ),
    CART_ITEM_QUANTITY_OVERFLOW(
            HttpStatus.BAD_REQUEST,
            "Cart item quantity would exceed the maximum supported value"
    ),
    VOUCHER_INVALID_CONFIGURATION(
            HttpStatus.BAD_REQUEST,
            "Voucher configuration is invalid"
    ),
    VOUCHER_CODE_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "Voucher code already exists"
    ),
    VOUCHER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Voucher not found"
    ),
    ORDER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Order not found"
    ),
    ORDER_INVALID_STATUS_TRANSITION(
            HttpStatus.CONFLICT,
            "Order status transition is invalid"
    ),
    ORDER_INVALID_IDEMPOTENCY_KEY(
            HttpStatus.BAD_REQUEST,
            "Idempotency-Key must contain between 1 and 100 characters"
    ),
    ORDER_IDEMPOTENCY_CONFLICT(
            HttpStatus.CONFLICT,
            "Idempotency-Key has already been used for a different checkout"
    ),
    ORDER_CART_EMPTY(
            HttpStatus.BAD_REQUEST,
            "Cart is empty"
    ),
    ORDER_PRODUCT_UNAVAILABLE(
            HttpStatus.CONFLICT,
            "A product in the cart is no longer available"
    ),
    ORDER_AMOUNT_OUT_OF_RANGE(
            HttpStatus.BAD_REQUEST,
            "Cart total exceeds the supported amount"
    ),
    ORDER_VOUCHER_UNAVAILABLE(
            HttpStatus.CONFLICT,
            "Voucher cannot be applied to this order"
    ),
    ORDER_INSUFFICIENT_STOCK(
            HttpStatus.CONFLICT,
            "A product in the cart cannot be reserved in the requested quantity"
    ),

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Request validation failed"),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "Request body is invalid"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");

    private final HttpStatus status;
    private final String defaultMessage;
}
