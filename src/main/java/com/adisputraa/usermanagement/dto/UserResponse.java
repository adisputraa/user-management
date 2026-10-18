package com.adisputraa.usermanagement.dto;

import com.adisputraa.usermanagement.enums.Role;
import com.adisputraa.usermanagement.enums.Status;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        String address,
        Role role,
        Status status,
        Instant createdAt
) {
}
