package com.adisputraa.usermanagement.api;

import com.adisputraa.usermanagement.dto.UpdateUserRoleRequest;
import com.adisputraa.usermanagement.dto.UpdateUserStatusRequest;
import com.adisputraa.usermanagement.entity.User;
import com.adisputraa.usermanagement.enums.Role;
import com.adisputraa.usermanagement.enums.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminApiIntegrationTest extends IntegrationTestSupport {

    private static final String ADMIN_ID = "kc-admin";

    @Test
    @DisplayName("a CUSTOMER token cannot access admin endpoints (403)")
    void listUsers_forbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/admin/users").with(tokenFor("kc-alice", Role.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("admins get a paginated list of users")
    void listUsers_success() throws Exception {
        saveUser("admin", ADMIN_ID, Role.ADMIN);
        saveUser("alice", "kc-alice", Role.CUSTOMER);
        saveUser("bob", "kc-bob", Role.MERCHANT);

        mockMvc.perform(get("/admin/users?size=2").with(tokenFor(ADMIN_ID, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    @DisplayName("an unknown sort field returns 400 instead of 500")
    void listUsers_invalidSort() throws Exception {
        mockMvc.perform(get("/admin/users?sort=password").with(tokenFor(ADMIN_ID, Role.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("an unknown user ID returns 404")
    void getUser_notFound() throws Exception {
        mockMvc.perform(get("/admin/users/" + UUID.randomUUID()).with(tokenFor(ADMIN_ID, Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("deactivating a user updates the database and disables the Keycloak account")
    void updateStatus_deactivates() throws Exception {
        User alice = saveUser("alice", "kc-alice", Role.CUSTOMER);

        mockMvc.perform(put("/admin/users/" + alice.getId() + "/status")
                        .with(tokenFor(ADMIN_ID, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateUserStatusRequest(Status.INACTIVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        verify(keycloakUserClient).setEnabled("kc-alice", false);
    }

    @Test
    @DisplayName("changing a role replaces the realm role in Keycloak")
    void updateRole_success() throws Exception {
        User alice = saveUser("alice", "kc-alice", Role.CUSTOMER);

        mockMvc.perform(put("/admin/users/" + alice.getId() + "/role")
                        .with(tokenFor(ADMIN_ID, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateUserRoleRequest(Role.MERCHANT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MERCHANT"));

        verify(keycloakUserClient).replaceRealmRole("kc-alice", Role.CUSTOMER, Role.MERCHANT);
    }

    @Test
    @DisplayName("admins cannot delete themselves (409)")
    void deleteUser_rejectsSelf() throws Exception {
        User admin = saveUser("admin", ADMIN_ID, Role.ADMIN);

        mockMvc.perform(delete("/admin/users/" + admin.getId()).with(tokenFor(ADMIN_ID, Role.ADMIN)))
                .andExpect(status().isConflict());

        verify(keycloakUserClient, never()).deleteUser(anyString());
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("deleting a user removes it from the database and Keycloak")
    void deleteUser_success() throws Exception {
        User alice = saveUser("alice", "kc-alice", Role.CUSTOMER);

        mockMvc.perform(delete("/admin/users/" + alice.getId()).with(tokenFor(ADMIN_ID, Role.ADMIN)))
                .andExpect(status().isNoContent());

        verify(keycloakUserClient).deleteUser("kc-alice");
        assertThat(userRepository.findById(alice.getId())).isEmpty();
    }
}
