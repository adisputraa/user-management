package com.adisputraa.usermanagement.api;

import com.adisputraa.usermanagement.dto.UpdateProfileRequest;
import com.adisputraa.usermanagement.enums.Role;
import com.adisputraa.usermanagement.exception.IdentityProviderException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileApiIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("requests without an access token are rejected with 401")
    void getProfile_withoutToken() throws Exception {
        mockMvc.perform(get("/profiles/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("returns the profile that belongs to the token's subject")
    void getProfile_success() throws Exception {
        saveUser("alice", "kc-alice", Role.CUSTOMER);
        saveUser("bob", "kc-bob", Role.CUSTOMER);

        mockMvc.perform(get("/profiles/me").with(tokenFor("kc-alice", Role.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    @DisplayName("update changes the profile in the database and the name in Keycloak")
    void updateProfile_success() throws Exception {
        saveUser("alice", "kc-alice", Role.CUSTOMER);
        UpdateProfileRequest request = new UpdateProfileRequest("Alicia", "Smith", "0800", "Bandung");

        mockMvc.perform(put("/profiles/me")
                        .with(tokenFor("kc-alice", Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alicia"))
                .andExpect(jsonPath("$.address").value("Bandung"));

        verify(keycloakUserClient).updateName("kc-alice", "Alicia", "Smith");
    }

    @Test
    @DisplayName("a Keycloak failure during update rolls the database change back")
    void updateProfile_rollsBackWhenKeycloakFails() throws Exception {
        saveUser("alice", "kc-alice", Role.CUSTOMER);
        doThrow(new IdentityProviderException("down"))
                .when(keycloakUserClient).updateName("kc-alice", "Alicia", "Smith");

        mockMvc.perform(put("/profiles/me")
                        .with(tokenFor("kc-alice", Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateProfileRequest("Alicia", "Smith", null, null))))
                .andExpect(status().isBadGateway());

        assertThat(userRepository.findWithProfileByKeycloakId("kc-alice").orElseThrow()
                .getProfile().getFirstName()).isEqualTo("First");
    }

    @Test
    @DisplayName("delete removes the account from the database and Keycloak")
    void deleteAccount_success() throws Exception {
        saveUser("alice", "kc-alice", Role.CUSTOMER);

        mockMvc.perform(delete("/profiles/me").with(tokenFor("kc-alice", Role.CUSTOMER)))
                .andExpect(status().isNoContent());

        verify(keycloakUserClient).deleteUser("kc-alice");
        assertThat(userRepository.count()).isZero();
    }
}
