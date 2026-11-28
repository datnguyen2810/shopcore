package io.github.datnguyen2810.shopcore;

import io.github.datnguyen2810.shopcore.auth.RefreshTokenCleanupJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Cùng cấu hình với OrderCheckoutTests để dùng chung Spring context và container PostgreSQL
@SpringBootTest(properties = "app.scheduling.enabled=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RefreshTokenCleanupTests {

    @Autowired
    private RefreshTokenCleanupJob cleanupJob;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void deleteExpiredTokens_removesOnlyFamiliesExpiredMoreThanOneDayAgo() {
        UUID userId = createUser();
        // Family đã rotate (token cũ trỏ sang token mới), hết hạn 2 ngày trước -> bị xoá trọn
        UUID oldFamily = UUID.randomUUID();
        UUID latest = insertToken(userId, oldFamily, Duration.ofDays(-2), null);
        insertToken(userId, oldFamily, Duration.ofDays(-2), latest);
        // Mới hết hạn 1 giờ và còn hạn 1 ngày -> giữ lại
        UUID recentlyExpired = insertToken(userId, UUID.randomUUID(), Duration.ofHours(-1), null);
        UUID active = insertToken(userId, UUID.randomUUID(), Duration.ofDays(1), null);

        cleanupJob.deleteExpiredTokens();

        assertThat(jdbc.queryForList("SELECT id FROM refresh_tokens WHERE user_id = ?", UUID.class, userId))
                .containsExactlyInAnyOrder(recentlyExpired, active);
    }

    private UUID insertToken(UUID userId, UUID familyId, Duration expiresIn, UUID replacedBy) {
        UUID id = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(expiresIn);
        Timestamp createdAt = Timestamp.from(expiresAt.minus(Duration.ofDays(7)));
        jdbc.update("""
                INSERT INTO refresh_tokens
                    (id, user_id, token_hash, family_id, expires_at, revoked_at, replaced_by_token_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, id, userId, "hash-" + id, familyId, Timestamp.from(expiresAt),
                replacedBy == null ? null : createdAt, replacedBy, createdAt);
        return id;
    }

    private UUID createUser() {
        UUID id = UUID.randomUUID();
        String username = "cleanup_" + id.toString().replace("-", "");
        jdbc.update("""
                INSERT INTO users (id, email, username, password_hash, full_name)
                VALUES (?, ?, ?, 'test-password-hash', 'Cleanup Test')
                """, id, username + "@example.com", username);
        return id;
    }
}
