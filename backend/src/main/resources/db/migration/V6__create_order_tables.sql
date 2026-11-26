CREATE TABLE orders (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    order_number VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    subtotal NUMERIC(19, 2) NOT NULL,
    discount_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(19, 2) NOT NULL,
    voucher_code VARCHAR(80),
    recipient_name VARCHAR(150) NOT NULL,
    recipient_phone VARCHAR(32) NOT NULL,
    address_line VARCHAR(255) NOT NULL,
    ward VARCHAR(120) NOT NULL,
    district VARCHAR(120) NOT NULL,
    city VARCHAR(120) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    payment_expires_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT uk_orders_order_number UNIQUE (order_number),
    CONSTRAINT uk_orders_user_idempotency_key UNIQUE (user_id, idempotency_key),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_orders_order_number_not_blank CHECK (BTRIM(order_number) <> ''),
    CONSTRAINT ck_orders_status
        CHECK (status IN (
            'PENDING_PAYMENT', 'CONFIRMED', 'PROCESSING', 'SHIPPING', 'COMPLETED',
            'CANCELLED', 'EXPIRED'
        )),
    CONSTRAINT ck_orders_amounts
        CHECK (
            subtotal >= 0
            AND discount_amount BETWEEN 0 AND subtotal
            AND total_amount = subtotal - discount_amount
        ),
    CONSTRAINT ck_orders_idempotency_key_not_blank CHECK (BTRIM(idempotency_key) <> ''),
    CONSTRAINT ck_orders_request_hash_format CHECK (request_hash ~ '^[0-9A-Fa-f]{64}$'),
    CONSTRAINT ck_orders_payment_expiration CHECK (payment_expires_at > created_at),
    CONSTRAINT ck_orders_version_non_negative CHECK (version >= 0)
);

CREATE INDEX idx_orders_user_created_at ON orders (user_id, created_at DESC);

CREATE INDEX idx_orders_pending_payment_expires_at ON orders (payment_expires_at)
    WHERE status = 'PENDING_PAYMENT';

CREATE TABLE order_items (
    id UUID NOT NULL,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    sku VARCHAR(80) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    line_total NUMERIC(19, 2) NOT NULL,

    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT uk_order_items_order_product UNIQUE (order_id, product_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders (id) ON DELETE RESTRICT,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT ck_order_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_order_items_amounts
        CHECK (unit_price >= 0 AND line_total = unit_price * quantity)
);

CREATE INDEX idx_order_items_product_id ON order_items (product_id);

CREATE TABLE order_status_history (
    id UUID NOT NULL,
    order_id UUID NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_by UUID,
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_order_status_history PRIMARY KEY (id),
    CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id)
        REFERENCES orders (id) ON DELETE RESTRICT,
    CONSTRAINT fk_order_status_history_changed_by FOREIGN KEY (changed_by)
        REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_order_status_history_from_status
        CHECK (from_status IN (
            'PENDING_PAYMENT', 'CONFIRMED', 'PROCESSING', 'SHIPPING', 'COMPLETED',
            'CANCELLED', 'EXPIRED'
        )),
    CONSTRAINT ck_order_status_history_to_status
        CHECK (to_status IN (
            'PENDING_PAYMENT', 'CONFIRMED', 'PROCESSING', 'SHIPPING', 'COMPLETED',
            'CANCELLED', 'EXPIRED'
        )),
    CONSTRAINT ck_order_status_history_status_changed CHECK (from_status <> to_status)
);

CREATE INDEX idx_order_status_history_order_created_at
    ON order_status_history (order_id, created_at);

ALTER TABLE inventory_transactions
    ADD CONSTRAINT fk_inventory_transactions_order FOREIGN KEY (order_id)
        REFERENCES orders (id) ON DELETE RESTRICT;
