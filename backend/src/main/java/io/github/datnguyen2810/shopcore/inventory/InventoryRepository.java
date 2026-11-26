package io.github.datnguyen2810.shopcore.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {


    @Query("""
        SELECT i
        FROM Inventory i
        WHERE i.productId = :productId
          AND EXISTS (
              SELECT p.id
              FROM Product p
              WHERE p.id = i.productId
                AND p.deletedAt IS NULL
          )
        """)
    Optional<Inventory> findByProductIdAndProductNotDeleted(
            @Param("productId") UUID productId
    );

    @Query("""
        SELECT i
        FROM Inventory i
        WHERE EXISTS (
            SELECT p.id
            FROM Product p
            WHERE p.id = i.productId
              AND p.deletedAt IS NULL
        )
        """)
    Page<Inventory> findAllByProductNotDeleted(Pageable pageable);

    // atomic conditional update (cập nhật nguyên tử có điều kiện)
    @Modifying
    @Query(value = """
            UPDATE inventories
            SET available_quantity = available_quantity - :quantity,
                reserved_quantity = reserved_quantity + :quantity,
                version = version + 1, -- tăng version để entity Inventory (có @Version) bên phía admin phát hiện được là dữ liệu vừa bị thay đổi
                updated_at = :now
            WHERE product_id = :productId
              AND available_quantity >= :quantity
              AND reserved_quantity <= 2147483647 - :quantity -- số int
            """, nativeQuery = true)
    int reserve(@Param("productId") UUID productId, @Param("quantity") int quantity, @Param("now") Instant now);

    // Ngược lại với reserve: trả hàng đang giữ về available (đơn bị huỷ / hết hạn thanh toán)
    @Modifying
    @Query(value = """
            UPDATE inventories
            SET available_quantity = available_quantity + :quantity,
                reserved_quantity = reserved_quantity - :quantity,
                version = version + 1,
                updated_at = :now
            WHERE product_id = :productId
              AND reserved_quantity >= :quantity
              AND available_quantity <= 2147483647 - :quantity
            """, nativeQuery = true)
    int release(@Param("productId") UUID productId, @Param("quantity") int quantity, @Param("now") Instant now);

    // Hàng đang giữ rời kho khi giao (SHIPPING): trừ hẳn khỏi reserved, available không đổi
    @Modifying
    @Query(value = """
            UPDATE inventories
            SET reserved_quantity = reserved_quantity - :quantity,
                version = version + 1,
                updated_at = :now
            WHERE product_id = :productId
              AND reserved_quantity >= :quantity
            """, nativeQuery = true)
    int sell(@Param("productId") UUID productId, @Param("quantity") int quantity, @Param("now") Instant now);
}
