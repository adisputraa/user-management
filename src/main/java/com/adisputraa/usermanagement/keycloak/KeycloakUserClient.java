package com.adisputraa.usermanagement.keycloak;

import com.adisputraa.usermanagement.config.KeycloakProperties;
import com.adisputraa.usermanagement.enums.Role;
import com.adisputraa.usermanagement.exception.ConflictException;
import com.adisputraa.usermanagement.exception.IdentityProviderException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Wraps the Keycloak Admin API. Keycloak owns credentials, the enabled flag, and realm roles;
 * every change to those goes through this class.
 */
@Component
public class KeycloakUserClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakUserClient.class);

    private final Keycloak keycloak;
    private final String realm;

    public KeycloakUserClient(Keycloak keycloakAdminClient, KeycloakProperties properties) {
        this.keycloak = keycloakAdminClient;
        this.realm = properties.realm();
    }

    /** Creates an enabled user with a permanent password and returns its Keycloak ID. */
    public String createUser(String username, String email, String password, String firstName, String lastName) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setCredentials(List.of(credential));

        return call(() -> {
            try (Response response = users().create(user)) {
                if (response.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                    throw new ConflictException("Username or email is already registered");
                }
                if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                    throw new IdentityProviderException("Keycloak rejected user creation with status " + response.getStatus());
                }
                // Keycloak returns the new user's URL in the Location header; the last segment is its ID.
                return CreatedResponseUtil.getCreatedId(response);
            }
        });
    }

    public Optional<String> findUserIdByUsername(String username) {
        return call(() -> users().searchByUsername(username, true).stream()
                .findFirst()
                .map(UserRepresentation::getId));
    }

    public void assignRealmRole(String userId, Role role) {
        run(() -> realmRoles(userId).add(List.of(roleRepresentation(role))));
    }

    public void replaceRealmRole(String userId, Role oldRole, Role newRole) {
        run(() -> {
            RoleScopeResource roles = realmRoles(userId);
            roles.remove(List.of(roleRepresentation(oldRole)));
            roles.add(List.of(roleRepresentation(newRole)));
        });
    }

    /** Disabled users cannot log in or refresh tokens. */
    public void setEnabled(String userId, boolean enabled) {
        run(() -> {
            UserResource resource = users().get(userId);
            UserRepresentation user = resource.toRepresentation();
            user.setEnabled(enabled);
            resource.update(user);
        });
    }

    public void updateName(String userId, String firstName, String lastName) {
        run(() -> {
            UserResource resource = users().get(userId);
            UserRepresentation user = resource.toRepresentation();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            resource.update(user);
        });
    }

    /** Deletes the user; a user that is already gone counts as deleted. */
    public void deleteUser(String userId) {
        run(() -> {
            try {
                users().get(userId).remove();
            } catch (NotFoundException e) {
                log.warn("Keycloak user {} was already deleted", userId);
            }
        });
    }

    /**
     * Best-effort delete used to undo a half-finished registration. It never throws, so the
     * original error reaches the caller; a failure is logged for manual clean-up.
     */
    public void deleteUserQuietly(String userId) {
        try {
            deleteUser(userId);
        } catch (RuntimeException e) {
            log.error("Could not delete orphaned Keycloak user {}; remove it manually", userId, e);
        }
    }

    private RoleScopeResource realmRoles(String userId) {
        return users().get(userId).roles().realmLevel();
    }

    private RoleRepresentation roleRepresentation(Role role) {
        return keycloak.realm(realm).roles().get(role.name()).toRepresentation();
    }

    private UsersResource users() {
        return keycloak.realm(realm).users();
    }

    private void run(Runnable action) {
        call(() -> {
            action.run();
            return null;
        });
    }

    /** Translates JAX-RS client errors into the application's exception type. */
    private <T> T call(Supplier<T> action) {
        try {
            return action.get();
        } catch (WebApplicationException e) {
            throw new IdentityProviderException(
                    "Keycloak Admin API request failed with status " + e.getResponse().getStatus(), e);
        } catch (ProcessingException e) {
            throw new IdentityProviderException("Keycloak is unreachable", e);
        }
    }
}
