package com.bookworm.ebookstore.mapper;

import com.bookworm.ebookstore.dto.UserResponse;
import com.bookworm.ebookstore.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getGiftPointsBalance()
        );
    }
}
