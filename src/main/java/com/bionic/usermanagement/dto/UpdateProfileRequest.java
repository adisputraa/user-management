package com.bionic.usermanagement.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phoneNumber,
        String address
) {
}
