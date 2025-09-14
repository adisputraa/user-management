package com.bionic.usermanagement.service;

import com.bionic.usermanagement.dto.KeycloakTokenResponse;
import com.bionic.usermanagement.dto.LoginRequest;
import com.bionic.usermanagement.dto.RefreshTokenRequest;
import com.bionic.usermanagement.dto.RegisterRequest;
import com.bionic.usermanagement.enums.Role;
import com.bionic.usermanagement.enums.Status;
import com.bionic.usermanagement.entity.User;
import com.bionic.usermanagement.entity.UserProfile;
import com.bionic.usermanagement.repository.UserRepository;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final Keycloak keycloak;
    private final RestTemplate restTemplate;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.token-uri}")
    private String keycloakTokenUri;
    @Value("${keycloak.login-client-id}")
    private String loginClientId;
    @Value("${keycloak.login-client-secret}")
    private String loginClientSecret;

    public AuthService(UserRepository userRepository, Keycloak keycloak, RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.keycloak = keycloak;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public void registerUser(RegisterRequest request) {
        // validasi username or email
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already registered");
        }

        // create user di keycloak
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(request.username());
        userRepresentation.setEmail(request.email());
        userRepresentation.setFirstName(request.firstName());
        userRepresentation.setLastName(request.lastName());
        userRepresentation.setEnabled(true);
        userRepresentation.setEmailVerified(true);

        // set password user di keycloak
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.password());
        credential.setTemporary(false);
        userRepresentation.setCredentials(Collections.singletonList(credential));

        RealmResource realmResource = keycloak.realm(realm);
        UsersResource usersResource = realmResource.users();

        // kirim request untuk membuat user ke keycloak
        Response response = usersResource.create(userRepresentation);

        if (response.getStatus() != 201) {
            String errorMessage = response.readEntity(String.class);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create user in Keycloak: " + errorMessage);
        }

        // ambil ID user yang baru dibuat di keycloak
        String keyCloakUserId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

        // assign role default "CUSTOMER" ke user di keycloak
        RoleRepresentation customerRole = realmResource.roles().get(Role.CUSTOMER.name()).toRepresentation();
        usersResource.get(keyCloakUserId).roles().realmLevel().add(Collections.singletonList(customerRole));

        // simpan user dan profile ke database
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setKeycloakId(keyCloakUserId);
        user.setRole(Role.CUSTOMER);
        user.setStatus(Status.ACTIVE);
        user.setCreatedAt(Instant.now());

        UserProfile userProfile = new UserProfile();
        userProfile.setFirstName(request.firstName());
        userProfile.setLastName(request.lastName());
        userProfile.setPhoneNumber(request.phoneNumber());
        userProfile.setAddress(request.address());

        user.setUserProfile(userProfile);
        userProfile.setUser(user);

        userRepository.save(user);
    }

    public KeycloakTokenResponse login(LoginRequest loginRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", loginClientId);
        map.add("client_secret", loginClientSecret);
        map.add("grant_type", "password");
        map.add("username", loginRequest.username());
        map.add("password", loginRequest.password());

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<KeycloakTokenResponse> response =
                    restTemplate.postForEntity(keycloakTokenUri, entity, KeycloakTokenResponse.class);

            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login failed. Invalid username or password.");
        }
    }

    public KeycloakTokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", loginClientId);
        map.add("client_secret", loginClientSecret);
        map.add("grant_type", "refresh_token");
        map.add("refresh_token", refreshTokenRequest.refreshToken());

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);
        try {
            ResponseEntity<KeycloakTokenResponse> response = restTemplate.postForEntity(keycloakTokenUri, entity, KeycloakTokenResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Failed to refresh token. Invalid token.");
        }
    }

    public void logout(RefreshTokenRequest refreshTokenRequest) {

    }
}
