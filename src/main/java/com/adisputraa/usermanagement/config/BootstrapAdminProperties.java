package com.adisputraa.usermanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * First administrator account, created on startup when it does not exist yet.
 * Bound from the {@code app.bootstrap-admin.*} properties.
 *
 * @param waitTimeout how long to keep retrying while Keycloak is still starting
 */
@ConfigurationProperties(prefix = "app.bootstrap-admin")
public record BootstrapAdminProperties(
        boolean enabled,
        String username,
        String email,
        String password,
        @DefaultValue("90s") Duration waitTimeout
) {
}
