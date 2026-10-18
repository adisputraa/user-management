package com.adisputraa.usermanagement.service;

import com.adisputraa.usermanagement.dto.UserResponse;
import com.adisputraa.usermanagement.entity.User;
import com.adisputraa.usermanagement.enums.Role;
import com.adisputraa.usermanagement.enums.Status;
import com.adisputraa.usermanagement.exception.BadRequestException;
import com.adisputraa.usermanagement.exception.ConflictException;
import com.adisputraa.usermanagement.exception.ResourceNotFoundException;
import com.adisputraa.usermanagement.keycloak.KeycloakUserClient;
import com.adisputraa.usermanagement.mapper.UserMapper;
import com.adisputraa.usermanagement.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * User administration. Like {@link UserProfileService}, writes update the database and flush
 * first, then call Keycloak last, so a Keycloak failure rolls the database change back.
 */
@Service
public class AdminUserService {

    private static final Set<String> SORTABLE_FIELDS = new TreeSet<>(
            Set.of("createdAt", "username", "email", "role", "status"));

    private final UserRepository userRepository;
    private final KeycloakUserClient keycloakUserClient;

    public AdminUserService(UserRepository userRepository, KeycloakUserClient keycloakUserClient) {
        this.userRepository = userRepository;
        this.keycloakUserClient = keycloakUserClient;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(Pageable pageable) {
        // The sort is appended to a custom JPQL query, so unknown fields must be rejected up front.
        pageable.getSort().forEach(order -> {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException("Cannot sort by '" + order.getProperty()
                        + "'. Allowed: " + String.join(", ", SORTABLE_FIELDS));
            }
        });
        return userRepository.findAllWithProfile(pageable).map(UserMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID id) {
        return UserMapper.toResponse(findUser(id));
    }

    /** INACTIVE disables the Keycloak user, which blocks new logins and token refreshes. */
    @Transactional
    public UserResponse updateStatus(UUID id, Status status, String actingAdminKeycloakId) {
        User user = findUser(id);
        rejectSelf(user, actingAdminKeycloakId, "change their own status");
        if (user.getStatus() != status) {
            user.setStatus(status);
            userRepository.flush();
            keycloakUserClient.setEnabled(user.getKeycloakId(), status == Status.ACTIVE);
        }
        return UserMapper.toResponse(user);
    }

    /** The new role appears in the user's tokens from their next login or token refresh. */
    @Transactional
    public UserResponse updateRole(UUID id, Role role, String actingAdminKeycloakId) {
        User user = findUser(id);
        rejectSelf(user, actingAdminKeycloakId, "change their own role");
        Role oldRole = user.getRole();
        if (oldRole != role) {
            user.setRole(role);
            userRepository.flush();
            keycloakUserClient.replaceRealmRole(user.getKeycloakId(), oldRole, role);
        }
        return UserMapper.toResponse(user);
    }

    @Transactional
    public void deleteUser(UUID id, String actingAdminKeycloakId) {
        User user = findUser(id);
        rejectSelf(user, actingAdminKeycloakId, "delete themselves");
        userRepository.delete(user);
        userRepository.flush();
        keycloakUserClient.deleteUser(user.getKeycloakId());
    }

    private User findUser(UUID id) {
        return userRepository.findWithProfileById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));
    }

    /** Prevents an admin from locking themselves out (e.g. the last admin demoting itself). */
    private void rejectSelf(User user, String actingAdminKeycloakId, String action) {
        if (user.getKeycloakId().equals(actingAdminKeycloakId)) {
            throw new ConflictException("Admins cannot " + action + " through the admin API");
        }
    }
}
