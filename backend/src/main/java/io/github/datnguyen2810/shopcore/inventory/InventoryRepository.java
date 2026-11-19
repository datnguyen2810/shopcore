package io.github.datnguyen2810.shopcore.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
