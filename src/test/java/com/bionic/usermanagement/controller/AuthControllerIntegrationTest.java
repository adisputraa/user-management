package com.bionic.usermanagement.controller;

import com.bionic.usermanagement.dto.RegisterRequest;
import com.bionic.usermanagement.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;

    @MockBean private Keycloak keycloak;

    private final RealmResource realmResource = Mockito.mock(RealmResource.class);
    private final UsersResource usersResource = Mockito.mock(UsersResource.class);
    private final UserResource userResource = Mockito.mock(UserResource.class);
    private final RolesResource rolesResource = Mockito.mock(RolesResource.class);
    private final RoleMappingResource roleMappingResource = Mockito.mock(RoleMappingResource.class);
    private final RoleScopeResource roleScopeResource = Mockito.mock(RoleScopeResource.class);
    private final Response response = Mockito.mock(Response.class);

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(realmResource.roles()).thenReturn(rolesResource);
    }

    @Test
    @DisplayName("Skenario Sukses: Registrasi user baru dengan data valid")
    void registerUser_withValidData_shouldSucceedAndReturnCreated() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "testuser", "test@example.com", "password123",
                "Test", "User", null, null
        );

        // Setup Mock untuk `create`
        when(usersResource.create(any())).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getLocation()).thenReturn(new URI("/users/some-keycloak-id"));

        RoleResource mockRoleResource = Mockito.mock(RoleResource.class);
        RoleRepresentation customerRoleRepresentation = new RoleRepresentation();
        when(rolesResource.get("CUSTOMER")).thenReturn(mockRoleResource);
        when(mockRoleResource.toRepresentation()).thenReturn(customerRoleRepresentation);
        // 👆👆 INI MEMPERBAIKI ERROR 'thenReturn(RoleRepresentation)' 👆👆

        when(usersResource.get(anyString())).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(userRepository.existsByUsername("testuser")).isTrue();
    }
}