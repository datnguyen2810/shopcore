package io.github.datnguyen2810.shopcore.user;

import io.github.datnguyen2810.shopcore.common.exception.ErrorCode;
import io.github.datnguyen2810.shopcore.common.exception.ResourceNotFoundException;
import io.github.datnguyen2810.shopcore.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.USER_NOT_FOUND,
                        "User not found"
                ));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("User is not active");
        }

        return userMapper.toUserResponse(user);
    }


}
