package io.github.datnguyen2810.shopcore.user.dto;

import io.github.datnguyen2810.shopcore.user.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class UserResponse {
    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private UserStatus status;
    private Instant createdAt;
}
