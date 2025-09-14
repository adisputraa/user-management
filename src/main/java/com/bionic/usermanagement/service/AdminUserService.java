package com.bionic.usermanagement.service;

import com.bionic.usermanagement.dto.UpdateUserRequest;
import com.bionic.usermanagement.dto.UpdateUserStatusRequest;
import com.bionic.usermanagement.dto.UserProfileDto;
import com.bionic.usermanagement.enums.Status;
import com.bionic.usermanagement.exception.ResourceNotFoundException;
import com.bionic.usermanagement.entity.User;
import com.bionic.usermanagement.repository.UserRepository;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.UUID;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    public AdminUserService(UserRepository userRepository, Keycloak keycloak) {
        this.userRepository = userRepository;
        this.keycloak = keycloak;
    }

    public Page<UserProfileDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapToUserProfileDto);
    }

    public UserProfileDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + id + " not found"));
        return mapToUserProfileDto(user);
    }

    @Transactional
    public void updateUserStatus(UUID id, UpdateUserStatusRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + id + " not found"));

        // Update status di database
        user.setStatus(request.status());
        userRepository.save(user);

        // Update status (enabled/disabled) di Keycloak
        UserResource userResource = keycloak.realm(realm).users().get(user.getKeycloakId());
        UserRepresentation userRepresentation = userResource.toRepresentation();
        userRepresentation.setEnabled(request.status() == Status.ACTIVE);
        userResource.update(userRepresentation);
    }

    @Transactional
    public void updateUser(UUID id, UpdateUserRequest request) {
        User user = findUserById(id);

        com.bionic.usermanagement.enums.Role oldRole = user.getRole();

        user.setRole(request.role());
        userRepository.save(user);

        // Update role di Keycloak
        UserResource userResource = keycloak.realm(realm).users().get(user.getKeycloakId());

        // Hapus role lama
        RoleRepresentation oldRoleRep = keycloak.realm(realm).roles().get(oldRole.name()).toRepresentation();
        userResource.roles().realmLevel().remove(Collections.singletonList(oldRoleRep));

        // Tambah role baru
        RoleRepresentation newRoleRep = keycloak.realm(realm).roles().get(request.role().name()).toRepresentation();
        userResource.roles().realmLevel().add(Collections.singletonList(newRoleRep));
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = findUserById(id);

        keycloak.realm(realm).users().get(user.getKeycloakId()).remove();

        userRepository.delete(user);
    }

    private User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + id + " not found"));
    }

    // Helper method
    private UserProfileDto mapToUserProfileDto(User user) {
        return new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getUserProfile().getFirstName(),
                user.getUserProfile().getLastName(),
                user.getUserProfile().getPhoneNumber(),
                user.getUserProfile().getAddress(),
                user.getRole(),
                user.getStatus()
        );
    }
}
