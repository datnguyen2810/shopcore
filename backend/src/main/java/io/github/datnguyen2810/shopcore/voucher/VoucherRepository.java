package io.github.datnguyen2810.shopcore.voucher;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, UUID> {

    Optional<Voucher> findByCode(String code);

    // PESSIMISTIC_WRITE lấy quyền cập nhật ngay từ đầu và giữ đến khi transaction kết thúc. Hai endpoint GET vẫn dùng truy vấn thường
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Voucher v where v.id = :id")
    Optional<Voucher> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Query("update Voucher v set v.active = false where v.id = :id")
    int deactivateById(@Param("id") UUID id);

    // atomic conditional update (cập nhật nguyên tử có điều kiện)
    @Modifying
    @Query(value = """
            UPDATE vouchers
            SET used_count = used_count + 1
            WHERE id = :id
              AND active = TRUE
              AND starts_at <= :now
              AND expires_at > :now
              AND used_count < usage_limit
              AND minimum_order_value <= :subtotal
              AND discount_type = :discountType
              AND discount_value = :discountValue
              AND maximum_discount IS NOT DISTINCT FROM CAST(:maximumDiscount AS numeric)
            """, nativeQuery = true)
    int consume(@Param("id") UUID id, @Param("now") Instant now, @Param("subtotal") BigDecimal subtotal,
                @Param("discountType") String discountType, @Param("discountValue") BigDecimal discountValue,
                @Param("maximumDiscount") BigDecimal maximumDiscount);

    // Hoàn lại 1 lượt dùng khi đơn bị huỷ; code voucher không đổi sau khi tạo nên tìm theo code là an toàn
    @Modifying
    @Query(value = "UPDATE vouchers SET used_count = used_count - 1 WHERE code = :code AND used_count > 0",
            nativeQuery = true)
    int release(@Param("code") String code);
}
