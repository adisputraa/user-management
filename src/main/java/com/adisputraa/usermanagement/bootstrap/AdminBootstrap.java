package com.adisputraa.usermanagement.bootstrap;

import com.adisputraa.usermanagement.config.BootstrapAdminProperties;
import com.adisputraa.usermanagement.entity.User;
import com.adisputraa.usermanagement.entity.UserProfile;
import com.adisputraa.usermanagement.enums.Role;
import com.adisputraa.usermanagement.exception.IdentityProviderException;
import com.adisputraa.usermanagement.keycloak.KeycloakUserClient;
import com.adisputraa.usermanagement.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * Self-registration always grants CUSTOMER, so the first ADMIN is created here on startup,
 * in both Keycloak and the database. Does nothing when the admin already exists.
 *
 * <p>Keycloak takes ~30 seconds to start after {@code docker compose up}, so this waits for it
 * (up to {@code app.bootstrap-admin.wait-timeout}) instead of failing the application at once.
 */
@Component
@ConditionalOnProperty(prefix = "app.bootstrap-admin", name = "enabled", havingValue = "true")
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final Duration RETRY_INTERVAL = Duration.ofSeconds(3);

    private final BootstrapAdminProperties properties;
    private final UserRepository userRepository;
    private final KeycloakUserClient keycloakUserClient;

    public AdminBootstrap(BootstrapAdminProperties properties,
                          UserRepository userRepository,
                          KeycloakUserClient keycloakUserClient) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.keycloakUserClient = keycloakUserClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        String username = properties.username().toLowerCase(Locale.ROOT);
        String email = properties.email().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsername(username)) {
            log.info("Bootstrap admin '{}' already exists", username);
            return;
        }

        String keycloakId = ensureKeycloakAdmin(username, email);

        UserProfile profile = new UserProfile("System", "Administrator", null, null);
        userRepository.save(User.create(username, email, keycloakId, Role.ADMIN, profile));

        log.warn("Created bootstrap admin '{}'. Change its password before using this outside local development.",
                username);
    }

    /**
     * Creates (or reuses) the admin in Keycloak and grants ADMIN, retrying while Keycloak is
     * unreachable. Each attempt is safe to repeat: an existing user is found by username, and
     * granting a role the user already has is a no-op.
     */
    private String ensureKeycloakAdmin(String username, String email) {
        Instant deadline = Instant.now().plus(properties.waitTimeout());
        while (true) {
            try {
                String keycloakId = keycloakUserClient.findUserIdByUsername(username)
                        .orElseGet(() -> keycloakUserClient.createUser(
                                username, email, properties.password(), "System", "Administrator"));
                keycloakUserClient.assignRealmRole(keycloakId, Role.ADMIN);
                return keycloakId;
            } catch (IdentityProviderException e) {
                if (Instant.now().isAfter(deadline)) {
                    throw new IllegalStateException("Keycloak did not become available within "
                            + properties.waitTimeout().toSeconds() + "s. Is it running? Start it with "
                            + "'docker compose up -d', or set BOOTSTRAP_ADMIN_ENABLED=false.", e);
                }
                log.info("Waiting for Keycloak to start ({})", e.getMessage());
                sleep();
            }
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(RETRY_INTERVAL.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Keycloak", e);
        }
    }
}
