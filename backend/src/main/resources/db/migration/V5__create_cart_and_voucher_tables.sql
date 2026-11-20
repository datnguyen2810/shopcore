CREATE TABLE carts (
    id UUID NOT NULL,
    user_id UUID NOT NULL,

    CONSTRAINT pk_carts PRIMARY KEY (id),
    CONSTRAINT uk_carts_user UNIQUE (user_id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE cart_items (
    id UUID NOT NULL,
    cart_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,

    CONSTRAINT pk_cart_items PRIMARY KEY (id),
    CONSTRAINT uk_cart_items_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id)
        REFERENCES carts (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT ck_cart_items_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_cart_items_product_id ON cart_items (product_id);

CREATE TABLE vouchers (
    id UUID NOT NULL,
    code VARCHAR(80) NOT NULL,
    discount_type VARCHAR(30) NOT NULL,
    discount_value NUMERIC(19, 2) NOT NULL,
    minimum_order_value NUMERIC(19, 2) NOT NULL DEFAULT 0,
    maximum_discount NUMERIC(19, 2),
    usage_limit INTEGER NOT NULL,
    used_count INTEGER NOT NULL DEFAULT 0,
    starts_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_vouchers PRIMARY KEY (id),
    CONSTRAINT uk_vouchers_code UNIQUE (code),
    CONSTRAINT ck_vouchers_code_not_blank CHECK (BTRIM(code) <> ''),
    CONSTRAINT ck_vouchers_discount_type
        CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CONSTRAINT ck_vouchers_discount_value
        CHECK (discount_value > 0 AND (discount_type <> 'PERCENTAGE' OR discount_value <= 100)),
    CONSTRAINT ck_vouchers_minimum_order_value_non_negative CHECK (minimum_order_value >= 0),
    CONSTRAINT ck_vouchers_maximum_discount_positive CHECK (maximum_discount > 0),
    CONSTRAINT ck_vouchers_usage_limit_positive CHECK (usage_limit > 0),
    CONSTRAINT ck_vouchers_used_count CHECK (used_count BETWEEN 0 AND usage_limit),
    CONSTRAINT ck_vouchers_validity_period CHECK (expires_at > starts_at)
);
