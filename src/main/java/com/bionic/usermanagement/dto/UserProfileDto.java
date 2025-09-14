package com.bionic.usermanagement.dto;

import com.bionic.usermanagement.enums.Role;
import com.bionic.usermanagement.enums.Status;

import java.util.UUID;

public record UserProfileDto(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        String address,
        Role role,
        Status status
) {
}
