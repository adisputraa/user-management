package com.bionic.usermanagement.dto;

import com.bionic.usermanagement.enums.Status;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull Status status
) {
}
