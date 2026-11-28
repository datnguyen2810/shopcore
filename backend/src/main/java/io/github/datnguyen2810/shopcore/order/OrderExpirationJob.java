package io.github.datnguyen2810.shopcore.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Định kỳ chuyển các đơn quá hạn thanh toán sang EXPIRED để trả hàng về kho và hoàn lượt voucher. */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpirationJob {

    static final int BATCH_SIZE = 100;

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${app.scheduling.order-expiration-interval:1m}")
    public void expireOverdueOrders() {
        Instant now = Instant.now();
        List<UUID> orderIds = orderRepository.findOverduePendingPaymentIds(now, BATCH_SIZE);
        int expired = 0;
        for (UUID orderId : orderIds) {
            // Mỗi đơn một transaction (gọi qua OrderService để @Transactional có hiệu lực): một đơn lỗi không chặn đơn khác
            try {
                if (orderService.expireOrder(orderId, now)) {
                    expired++;
                }
            } catch (RuntimeException exception) {
                log.error("Failed to expire order {}", orderId, exception);
            }
        }
        if (expired > 0) {
            log.info("Expired {} overdue orders", expired);
        }
    }
}
