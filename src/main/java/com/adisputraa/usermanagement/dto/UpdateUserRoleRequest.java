package com.adisputraa.usermanagement.dto;

import com.adisputraa.usermanagement.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull Role role
) {
}
