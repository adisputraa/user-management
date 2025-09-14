package com.bionic.usermanagement.dto;

import com.bionic.usermanagement.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(
        @NotNull Role role
) {
}
