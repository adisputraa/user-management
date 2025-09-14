package com.bionic.usermanagement.service;

import com.bionic.usermanagement.dto.UpdateProfileRequest;
import com.bionic.usermanagement.dto.UserProfileDto;
import com.bionic.usermanagement.exception.ResourceNotFoundException;
import com.bionic.usermanagement.entity.User;
import com.bionic.usermanagement.repository.UserRepository;
import jakarta.transaction.Transactional;

import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    public UserProfileService(UserRepository userRepository, Keycloak keycloak) {
        this.userRepository = userRepository;
        this.keycloak = keycloak;
    }

    private User getUserByJwt(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("User with Keycloak ID " + keycloakId + " not found"));
    }

    public UserProfileDto getMyProfile(Jwt jwt) {
        User user = getUserByJwt(jwt);
        return mapToUserProfileDto(user);
    }

    @Transactional
    public UserProfileDto updateMyProfile(Jwt jwt, UpdateProfileRequest request) {
        User user = getUserByJwt(jwt);

        user.getUserProfile().setFirstName(request.firstName());
        user.getUserProfile().setLastName(request.lastName());
        user.getUserProfile().setPhoneNumber(request.phoneNumber());
        user.getUserProfile().setAddress(request.address());

        User updatedUser = userRepository.save(user);
        return mapToUserProfileDto(updatedUser);
    }

    @Transactional
    public void deleteMyProfile(Jwt jwt) {
        User user = getUserByJwt(jwt);

        keycloak.realm(realm).users().get(user.getKeycloakId()).remove();

        userRepository.delete(user);
    }

    // Helper method untuk mapping
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
