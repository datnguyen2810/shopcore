CREATE TABLE inventories (
    product_id UUID NOT NULL,
    available_quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_inventories PRIMARY KEY (product_id),
    CONSTRAINT fk_inventories_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT ck_inventories_available_quantity_non_negative
        CHECK (available_quantity >= 0),
    CONSTRAINT ck_inventories_reserved_quantity_non_negative
        CHECK (reserved_quantity >= 0),
    CONSTRAINT ck_inventories_version_non_negative CHECK (version >= 0)
);

-- Khởi tạo tồn kho bằng 0 cho các sản phẩm hiện có chưa bị xóa mềm.
INSERT INTO inventories (product_id)
SELECT id
FROM products
WHERE deleted_at IS NULL;

CREATE TABLE inventory_transactions (
    id UUID NOT NULL,
    product_id UUID NOT NULL,
    -- Thêm khóa ngoại tới bảng orders ở migration V6, khi bảng này được tạo.
    order_id UUID,
    -- Người thực hiện biến động kho (Admin hoặc Customer), không phải chủ sở hữu.
    -- Để NULL khi hệ thống tự thực hiện: scheduler hết hạn Order, payment webhook.
    created_by UUID,
    type VARCHAR(30) NOT NULL,
    -- ADJUST dùng số dương để tăng kho, số âm để giảm kho. Các loại khác dùng số lượng dương.
    quantity INTEGER NOT NULL,
    -- Lý do hoặc chứng từ: bắt buộc với ADJUST, tùy chọn với IMPORT, NULL với loại gắn Order.
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_inventory_transactions PRIMARY KEY (id),
    CONSTRAINT fk_inventory_transactions_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_transactions_created_by FOREIGN KEY (created_by)
        REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_inventory_transactions_type
        CHECK (type IN ('IMPORT', 'RESERVE', 'RELEASE', 'SALE', 'ADJUST')),
    CONSTRAINT ck_inventory_transactions_quantity
        CHECK (
            (type = 'ADJUST' AND quantity <> 0)
            OR (type IN ('IMPORT', 'RESERVE', 'RELEASE', 'SALE') AND quantity > 0)
        ),
    CONSTRAINT ck_inventory_transactions_note_not_blank
        CHECK (note IS NULL OR BTRIM(note) <> ''),
    CONSTRAINT ck_inventory_transactions_adjust_requires_note
        CHECK (type <> 'ADJUST' OR note IS NOT NULL)
);

CREATE INDEX idx_inventory_transactions_product_created_at
    ON inventory_transactions (product_id, created_at DESC);

CREATE INDEX idx_inventory_transactions_order_id
    ON inventory_transactions (order_id);

-- Chưa tạo index cho created_by: chỉ thêm bằng migration mới khi có API lọc lịch sử kho theo người thực hiện.
