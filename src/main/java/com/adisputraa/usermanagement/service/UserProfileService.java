package com.adisputraa.usermanagement.service;

import com.adisputraa.usermanagement.dto.UpdateProfileRequest;
import com.adisputraa.usermanagement.dto.UserResponse;
import com.adisputraa.usermanagement.entity.User;
import com.adisputraa.usermanagement.entity.UserProfile;
import com.adisputraa.usermanagement.exception.ResourceNotFoundException;
import com.adisputraa.usermanagement.keycloak.KeycloakUserClient;
import com.adisputraa.usermanagement.mapper.UserMapper;
import com.adisputraa.usermanagement.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operations a logged-in user performs on their own account. The account is identified by the
 * access token's subject ({@code sub}), which is the Keycloak user ID.
 *
 * <p>Write methods change the database first and flush, then call Keycloak last. If the Keycloak
 * call fails, the exception rolls the database transaction back, so both sides stay in sync.
 */
@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final KeycloakUserClient keycloakUserClient;

    public UserProfileService(UserRepository userRepository, KeycloakUserClient keycloakUserClient) {
        this.userRepository = userRepository;
        this.keycloakUserClient = keycloakUserClient;
    }

    @Transactional(readOnly = true)
    public UserResponse getMyProfile(String keycloakId) {
        return UserMapper.toResponse(findByKeycloakId(keycloakId));
    }

    @Transactional
    public UserResponse updateMyProfile(String keycloakId, UpdateProfileRequest request) {
        User user = findByKeycloakId(keycloakId);
        UserProfile profile = user.getProfile();
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setPhoneNumber(request.phoneNumber());
        profile.setAddress(request.address());
        userRepository.flush();

        // Keycloak also stores the name (it appears in tokens and the account console).
        keycloakUserClient.updateName(keycloakId, request.firstName(), request.lastName());
        return UserMapper.toResponse(user);
    }

    @Transactional
    public void deleteMyAccount(String keycloakId) {
        User user = findByKeycloakId(keycloakId);
        userRepository.delete(user);
        userRepository.flush();

        keycloakUserClient.deleteUser(keycloakId);
    }

    private User findByKeycloakId(String keycloakId) {
        return userRepository.findWithProfileByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("No local account for the current user"));
    }
}
