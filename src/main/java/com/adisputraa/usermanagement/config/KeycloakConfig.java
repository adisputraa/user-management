package com.adisputraa.usermanagement.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class KeycloakConfig {

    /**
     * Keycloak Admin API client. It authenticates as the "user-management-admin" service account
     * (client_credentials grant) and is only used to manage users, never to log users in.
     */
    @Bean
    public Keycloak keycloakAdminClient(KeycloakProperties properties) {
        return KeycloakBuilder.builder()
                .serverUrl(properties.serverUrl())
                .realm(properties.realm())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(properties.adminClient().clientId())
                .clientSecret(properties.adminClient().clientSecret())
                .build();
    }

    /** HTTP client for Keycloak's OpenID Connect token and logout endpoints. */
    @Bean
    public RestClient keycloakRestClient(RestClient.Builder builder) {
        return builder.build();
    }
}
