package com.adisputraa.usermanagement.service;

import com.adisputraa.usermanagement.dto.LoginRequest;
import com.adisputraa.usermanagement.dto.RefreshTokenRequest;
import com.adisputraa.usermanagement.dto.RegisterRequest;
import com.adisputraa.usermanagement.dto.TokenResponse;
import com.adisputraa.usermanagement.dto.UserResponse;
import com.adisputraa.usermanagement.entity.User;
import com.adisputraa.usermanagement.entity.UserProfile;
import com.adisputraa.usermanagement.enums.Role;
import com.adisputraa.usermanagement.exception.ConflictException;
import com.adisputraa.usermanagement.keycloak.KeycloakTokenClient;
import com.adisputraa.usermanagement.keycloak.KeycloakUserClient;
import com.adisputraa.usermanagement.mapper.UserMapper;
import com.adisputraa.usermanagement.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final KeycloakUserClient keycloakUserClient;
    private final KeycloakTokenClient keycloakTokenClient;

    public AuthService(UserRepository userRepository,
                       KeycloakUserClient keycloakUserClient,
                       KeycloakTokenClient keycloakTokenClient) {
        this.userRepository = userRepository;
        this.keycloakUserClient = keycloakUserClient;
        this.keycloakTokenClient = keycloakTokenClient;
    }

    /**
     * Creates the account in Keycloak (credentials + CUSTOMER role) and the local user/profile rows.
     * Keycloak is outside the database transaction, so if a later step fails the Keycloak user is
     * deleted again; otherwise it would be able to log in without a local record.
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        // Keycloak stores usernames and emails in lower case; keep the local copy identical.
        String username = request.username().toLowerCase(Locale.ROOT);
        String email = request.email().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }

        String keycloakId = keycloakUserClient.createUser(
                username, email, request.password(), request.firstName(), request.lastName());
        try {
            keycloakUserClient.assignRealmRole(keycloakId, Role.CUSTOMER);

            UserProfile profile = new UserProfile(
                    request.firstName(), request.lastName(), request.phoneNumber(), request.address());
            User user = User.create(username, email, keycloakId, Role.CUSTOMER, profile);
            // Flush now so constraint violations surface here, while compensation is still possible.
            return UserMapper.toResponse(userRepository.saveAndFlush(user));
        } catch (RuntimeException e) {
            log.warn("Registration of '{}' failed after the Keycloak user was created; removing it", username);
            keycloakUserClient.deleteUserQuietly(keycloakId);
            throw e;
        }
    }

    public TokenResponse login(LoginRequest request) {
        return keycloakTokenClient.login(request.username(), request.password());
    }

    public TokenResponse refresh(RefreshTokenRequest request) {
        return keycloakTokenClient.refresh(request.refreshToken());
    }

    public void logout(RefreshTokenRequest request) {
        keycloakTokenClient.logout(request.refreshToken());
    }
}
