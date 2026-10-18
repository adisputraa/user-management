package com.adisputraa.usermanagement.mapper;

import com.adisputraa.usermanagement.dto.UserResponse;
import com.adisputraa.usermanagement.entity.User;
import com.adisputraa.usermanagement.entity.UserProfile;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        UserProfile profile = user.getProfile();
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getPhoneNumber(),
                profile.getAddress(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
