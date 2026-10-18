package com.adisputraa.usermanagement.dto;

import com.adisputraa.usermanagement.enums.Status;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull Status status
) {
}
