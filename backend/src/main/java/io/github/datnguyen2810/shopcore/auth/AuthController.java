package io.github.datnguyen2810.shopcore.auth;

import io.github.datnguyen2810.shopcore.auth.dto.*;
import io.github.datnguyen2810.shopcore.common.response.ApiResponse;
import io.github.datnguyen2810.shopcore.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@RequestBody @Valid RegisterRequest request) {
        UserResponse registeredUser = authService.register(request);

        ApiResponse<UserResponse> reponse = ApiResponse.of(HttpStatus.CREATED, "User registered successfully", registeredUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse result = authService.login(request);

        ApiResponse<LoginResponse> reponse = ApiResponse.of(HttpStatus.OK, "Login successfully", result);
        return ResponseEntity.ok(reponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        LoginResponse result = authService.refresh(request);

        ApiResponse<LoginResponse> reponse = ApiResponse.of(HttpStatus.OK, "Token refreshed successfully", result);
        return ResponseEntity.ok(reponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal Jwt jwt, @RequestBody @Valid LogoutRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());

        authService.logout(userId, request);

        ApiResponse<Void> response = ApiResponse.message(HttpStatus.OK, "Logout successfully");
        return ResponseEntity.ok(response);
    }
}
