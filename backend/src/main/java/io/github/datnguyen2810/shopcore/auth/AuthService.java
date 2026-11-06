package io.github.datnguyen2810.shopcore.auth;

import io.github.datnguyen2810.shopcore.auth.dto.*;
import io.github.datnguyen2810.shopcore.user.dto.UserResponse;
import jakarta.validation.Valid;

import java.util.UUID;

public interface AuthService {
    UserResponse register(RegisterRequest request);

    LoginResponse login(@Valid LoginRequest request);

    LoginResponse refresh(RefreshTokenRequest request);

    void logout(UUID userId, LogoutRequest request);
}
