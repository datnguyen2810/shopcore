package io.github.datnguyen2810.shopcore.voucher;

import io.github.datnguyen2810.shopcore.voucher.dto.VoucherResponse;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, UUID> {

    // PESSIMISTIC_WRITE lấy quyền cập nhật ngay từ đầu và giữ đến khi transaction kết thúc. Hai endpoint GET vẫn dùng truy vấn thường
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Voucher v where v.id = :id")
    Optional<Voucher> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Query("update Voucher v set v.active = false where v.id = :id")
    int deactivateById(@Param("id") UUID id);
}
