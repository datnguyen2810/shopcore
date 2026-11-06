package io.github.datnguyen2810.shopcore.user;

import io.github.datnguyen2810.shopcore.user.dto.UserResponse;

import java.util.UUID;

public interface UserService {

    UserResponse getCurrentUser(UUID userId);
}
