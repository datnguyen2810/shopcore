package io.github.datnguyen2810.shopcore.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

    Page<Order> findByUserId(UUID userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(UUID userId, OrderStatus status, Pageable pageable);

    // Lọc theo cả userId để user không đọc được đơn của người khác; fetch items luôn trong 1 query.
    @EntityGraph(attributePaths = "items")
    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    // SELECT ... FOR UPDATE: các thao tác đổi trạng thái cùng một đơn (huỷ, thanh toán, hết hạn) phải xếp hàng.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id and o.userId = :userId")
    Optional<Order> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    // ===== Admin =====

    // Mỗi bộ lọc null thì bỏ qua (cùng pattern với ProductRepository.searchPublicProducts)
    @Query("""
            select o from Order o
            where (:status is null or o.status = :status)
              and (:userId is null or o.userId = :userId)
              and (:orderNumber is null or o.orderNumber = :orderNumber)
            """)
    Page<Order> search(@Param("status") OrderStatus status, @Param("userId") UUID userId,
                       @Param("orderNumber") String orderNumber, Pageable pageable);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") UUID id);

    // ===== Scheduler =====

    // Ghi thẳng 'PENDING_PAYMENT' trong SQL để PostgreSQL dùng partial index idx_orders_pending_payment_expires_at (V6)
    @Query(value = """
            SELECT id FROM orders
            WHERE status = 'PENDING_PAYMENT' AND payment_expires_at <= :now
            ORDER BY payment_expires_at
            LIMIT :limit
            """, nativeQuery = true)
    List<UUID> findOverduePendingPaymentIds(@Param("now") Instant now, @Param("limit") int limit);
}
