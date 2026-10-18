package com.adisputraa.usermanagement.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Connection settings for Keycloak, bound from the {@code keycloak.*} properties.
 *
 * @param serverUrl   base URL of the Keycloak server, e.g. http://localhost:8081
 * @param realm       realm that holds the users, roles, and clients
 * @param adminClient service-account client used for the Admin API (create/update/delete users)
 * @param loginClient client used for the token endpoint (login, refresh, logout)
 */
@Validated
@ConfigurationProperties(prefix = "keycloak")
public record KeycloakProperties(
        @NotBlank String serverUrl,
        @NotBlank String realm,
        Client adminClient,
        Client loginClient
) {

    public record Client(@NotBlank String clientId, @NotBlank String clientSecret) {
    }

    public String tokenUri() {
        return realmUrl() + "/protocol/openid-connect/token";
    }

    public String logoutUri() {
        return realmUrl() + "/protocol/openid-connect/logout";
    }

    private String realmUrl() {
        return serverUrl + "/realms/" + realm;
    }
}
