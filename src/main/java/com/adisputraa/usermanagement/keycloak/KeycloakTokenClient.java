package com.adisputraa.usermanagement.keycloak;

import com.adisputraa.usermanagement.config.KeycloakProperties;
import com.adisputraa.usermanagement.dto.TokenResponse;
import com.adisputraa.usermanagement.exception.AuthenticationFailedException;
import com.adisputraa.usermanagement.exception.IdentityProviderException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Talks to Keycloak's OpenID Connect endpoints on behalf of end users.
 * The API never checks passwords itself; it forwards them to Keycloak and relays the tokens.
 */
@Component
public class KeycloakTokenClient {

    private final RestClient restClient;
    private final KeycloakProperties properties;

    public KeycloakTokenClient(RestClient keycloakRestClient, KeycloakProperties properties) {
        this.restClient = keycloakRestClient;
        this.properties = properties;
    }

    /** Exchanges username and password for tokens (OAuth2 "password" grant). */
    public TokenResponse login(String username, String password) {
        MultiValueMap<String, String> form = clientForm();
        form.add("grant_type", "password");
        form.add("username", username);
        form.add("password", password);
        return requestTokens(form, "Invalid username or password");
    }

    /** Issues a new access token from a refresh token. */
    public TokenResponse refresh(String refreshToken) {
        MultiValueMap<String, String> form = clientForm();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        return requestTokens(form, "Invalid or expired refresh token");
    }

    /**
     * Ends the Keycloak session behind the refresh token, so it can no longer be refreshed.
     * Access tokens already issued stay valid until they expire (see accessTokenLifespan).
     */
    public void logout(String refreshToken) {
        MultiValueMap<String, String> form = clientForm();
        form.add("refresh_token", refreshToken);
        try {
            restClient.post()
                    .uri(properties.logoutUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        throw new AuthenticationFailedException("Invalid or expired refresh token");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new IdentityProviderException("Keycloak logout failed with " + response.getStatusCode());
                    })
                    .toBodilessEntity();
        } catch (ResourceAccessException e) {
            throw new IdentityProviderException("Keycloak is unreachable", e);
        }
    }

    private TokenResponse requestTokens(MultiValueMap<String, String> form, String rejectedMessage) {
        try {
            return restClient.post()
                    .uri(properties.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    // 400/401 from the token endpoint mean bad credentials, a disabled account, or a bad token.
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        throw new AuthenticationFailedException(rejectedMessage);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new IdentityProviderException("Keycloak token endpoint failed with " + response.getStatusCode());
                    })
                    .body(TokenResponse.class);
        } catch (ResourceAccessException e) {
            throw new IdentityProviderException("Keycloak is unreachable", e);
        }
    }

    private MultiValueMap<String, String> clientForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.loginClient().clientId());
        form.add("client_secret", properties.loginClient().clientSecret());
        return form;
    }
}
