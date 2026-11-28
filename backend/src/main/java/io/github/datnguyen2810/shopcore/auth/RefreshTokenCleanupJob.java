package io.github.datnguyen2810.shopcore.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/** Xoá refresh token đã hết hạn quá 1 ngày; token hết hạn đã bị từ chối nên không còn cần cho reuse detection. */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

    static final Duration RETENTION = Duration.ofDays(1);

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "${app.scheduling.refresh-token-cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void deleteExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredBefore(Instant.now().minus(RETENTION));
        if (deleted > 0) {
            log.info("Deleted {} expired refresh tokens", deleted);
        }
    }
}
