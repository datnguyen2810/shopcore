package io.github.datnguyen2810.shopcore.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    boolean existsBySku(String normalizedSku);

    boolean existsBySlug(String normalizedSlug);

    boolean existsBySkuAndIdNot(String normalizedSku, UUID id);

    boolean existsBySlugAndIdNot(String normalizedSlug, UUID id);

    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Product p
            SET p.deletedAt = :deletedAt,
                p.status = :status,
                p.updatedAt = :deletedAt,
                p.version = p.version + 1
            WHERE p.id = :id
              AND p.deletedAt IS NULL
            """)
    int softDeleteById(@Param("id") UUID id,
                       @Param("status") ProductStatus status,
                       @Param("deletedAt") Instant deletedAt);

    // Public product list
    @EntityGraph(attributePaths = {"category", "productImages"}) // tải Category và ảnh cùng Product, tránh mapper tạo nhiều query phụ.
    List<Product> findAllByStatusAndDeletedAtIsNullAndCategory_ActiveTrueOrderByCreatedAtDesc(ProductStatus productStatus);

    // Public product detail
    @EntityGraph(attributePaths = {"category", "productImages"})
    Optional<Product> findByIdAndStatusAndDeletedAtIsNullAndCategory_ActiveTrue(UUID id, ProductStatus productStatus);

    // countQuery -> tính totalElements, totalPages
    @Query(
            value = """
                    SELECT p
                    FROM Product p
                    JOIN FETCH p.category c
                    WHERE p.deletedAt IS NULL
                      AND p.status = :status
                      AND c.active = true
                      AND (
                            :keyword = ''
                            OR LOWER(p.name) LIKE CONCAT('%', :keyword, '%')
                            OR LOWER(p.sku) LIKE CONCAT('%', :keyword, '%')
                            OR LOWER(p.slug) LIKE CONCAT('%', :keyword, '%')
                      )
                      AND (
                            :categoryId IS NULL
                            OR c.id = :categoryId
                      )
                      AND (
                            :minPrice IS NULL
                            OR p.price >= :minPrice
                      )
                      AND (
                            :maxPrice IS NULL
                            OR p.price <= :maxPrice
                      )
                    """,
            countQuery = """
                    SELECT COUNT(p)
                    FROM Product p
                    JOIN p.category c
                    WHERE p.deletedAt IS NULL
                      AND p.status = :status
                      AND c.active = true
                      AND (
                            :keyword = ''
                            OR LOWER(p.name) LIKE CONCAT('%', :keyword, '%')
                            OR LOWER(p.sku) LIKE CONCAT('%', :keyword, '%')
                            OR LOWER(p.slug) LIKE CONCAT('%', :keyword, '%')
                      )
                      AND (
                            :categoryId IS NULL
                            OR c.id = :categoryId
                      )
                      AND (
                            :minPrice IS NULL
                            OR p.price >= :minPrice
                      )
                      AND (
                            :maxPrice IS NULL
                            OR p.price <= :maxPrice
                      )
                    """
    )
    Page<Product> searchPublicProducts(
            @Param("status") ProductStatus status,
            @Param("keyword") String keyword,
            @Param("categoryId") UUID categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );


}
