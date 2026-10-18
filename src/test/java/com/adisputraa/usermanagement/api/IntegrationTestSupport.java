package com.adisputraa.usermanagement.api;

import com.adisputraa.usermanagement.entity.User;
import com.adisputraa.usermanagement.entity.UserProfile;
import com.adisputraa.usermanagement.enums.Role;
import com.adisputraa.usermanagement.keycloak.KeycloakTokenClient;
import com.adisputraa.usermanagement.keycloak.KeycloakUserClient;
import com.adisputraa.usermanagement.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Shared setup: one PostgreSQL container for all test classes (started once, so the cached
 * Spring context always points at a live database) and mocks for both Keycloak clients.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationTestSupport {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    @MockitoBean
    KeycloakUserClient keycloakUserClient;

    @MockitoBean
    KeycloakTokenClient keycloakTokenClient;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    User saveUser(String username, String keycloakId, Role role) {
        UserProfile profile = new UserProfile("First", "Last", null, null);
        return userRepository.save(User.create(username, username + "@example.com", keycloakId, role, profile));
    }

    /** A request authenticated with a Keycloak-style JWT for the given subject and realm role. */
    static RequestPostProcessor tokenFor(String keycloakId, Role role) {
        return jwt()
                .jwt(token -> token.subject(keycloakId))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }
}
