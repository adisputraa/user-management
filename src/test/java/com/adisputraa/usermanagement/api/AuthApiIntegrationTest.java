package com.adisputraa.usermanagement.api;

import com.adisputraa.usermanagement.dto.LoginRequest;
import com.adisputraa.usermanagement.dto.RefreshTokenRequest;
import com.adisputraa.usermanagement.dto.RegisterRequest;
import com.adisputraa.usermanagement.dto.TokenResponse;
import com.adisputraa.usermanagement.enums.Role;
import com.adisputraa.usermanagement.exception.AuthenticationFailedException;
import com.adisputraa.usermanagement.exception.IdentityProviderException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiIntegrationTest extends IntegrationTestSupport {

    private static RegisterRequest validRegistration() {
        return new RegisterRequest("NewUser", "New.User@Example.com", "password123",
                "New", "User", "08123456789", "Jakarta");
    }

    @Test
    @DisplayName("register creates the Keycloak user with CUSTOMER role and stores a lower-cased local copy")
    void register_success() throws Exception {
        when(keycloakUserClient.createUser(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("kc-new");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRegistration())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("new.user@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(keycloakUserClient).createUser("newuser", "new.user@example.com", "password123", "New", "User");
        verify(keycloakUserClient).assignRealmRole("kc-new", Role.CUSTOMER);
        assertThat(userRepository.findWithProfileByKeycloakId("kc-new")).isPresent();
    }

    @Test
    @DisplayName("register rejects a taken username with 409 before touching Keycloak")
    void register_duplicateUsername() throws Exception {
        saveUser("newuser", "kc-existing", Role.CUSTOMER);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRegistration())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Username is already taken"));

        verify(keycloakUserClient, never()).createUser(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("register returns 400 with per-field errors for invalid input")
    void register_invalidInput() throws Exception {
        RegisterRequest invalid = new RegisterRequest("ab", "not-an-email", "short", "", "User", null, null);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.firstName").exists());
    }

    @Test
    @DisplayName("register deletes the Keycloak user again when a later step fails")
    void register_compensatesKeycloakUser() throws Exception {
        when(keycloakUserClient.createUser(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("kc-orphan");
        doThrow(new IdentityProviderException("role assignment failed"))
                .when(keycloakUserClient).assignRealmRole("kc-orphan", Role.CUSTOMER);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRegistration())))
                .andExpect(status().isBadGateway());

        verify(keycloakUserClient).deleteUserQuietly("kc-orphan");
        assertThat(userRepository.count()).isZero();
    }

    @Test
    @DisplayName("login returns the tokens issued by Keycloak")
    void login_success() throws Exception {
        when(keycloakTokenClient.login("newuser", "password123"))
                .thenReturn(new TokenResponse("access", "refresh", 300, 1800, "Bearer"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("newuser", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access"))
                .andExpect(jsonPath("$.refresh_token").value("refresh"))
                .andExpect(jsonPath("$.token_type").value("Bearer"));
    }

    @Test
    @DisplayName("login with wrong credentials returns 401")
    void login_invalidCredentials() throws Exception {
        when(keycloakTokenClient.login("newuser", "wrong-password"))
                .thenThrow(new AuthenticationFailedException("Invalid username or password"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("newuser", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
    }

    @Test
    @DisplayName("logout ends the Keycloak session and returns 204")
    void logout_success() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"refresh\"}"))
                .andExpect(status().isNoContent());

        verify(keycloakTokenClient).logout("refresh");
    }

    @Test
    @DisplayName("refresh with an invalid token returns 401")
    void refresh_invalidToken() throws Exception {
        when(keycloakTokenClient.refresh("expired"))
                .thenThrow(new AuthenticationFailedException("Invalid or expired refresh token"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RefreshTokenRequest("expired"))))
                .andExpect(status().isUnauthorized());
    }
}
