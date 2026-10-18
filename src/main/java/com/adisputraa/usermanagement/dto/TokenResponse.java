package com.adisputraa.usermanagement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Tokens issued by Keycloak, returned to the client unchanged. Lifetimes are in seconds. */
public record TokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("refresh_expires_in") long refreshExpiresIn,
        @JsonProperty("token_type") String tokenType
) {
}
