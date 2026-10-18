package com.adisputraa.usermanagement.controller;

import com.adisputraa.usermanagement.dto.UpdateProfileRequest;
import com.adisputraa.usermanagement.dto.UserResponse;
import com.adisputraa.usermanagement.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The current user's own account. The user is identified by the token's {@code sub} claim. */
@RestController
@RequestMapping("/profiles/me")
@Tag(name = "My profile", description = "Any authenticated user")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    @Operation(summary = "Get my profile")
    public UserResponse getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return userProfileService.getMyProfile(jwt.getSubject());
    }

    @PutMapping
    @Operation(summary = "Update my name, phone number, and address")
    public UserResponse updateMyProfile(@AuthenticationPrincipal Jwt jwt,
                                        @Valid @RequestBody UpdateProfileRequest request) {
        return userProfileService.updateMyProfile(jwt.getSubject(), request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete my account from Keycloak and the database")
    public void deleteMyAccount(@AuthenticationPrincipal Jwt jwt) {
        userProfileService.deleteMyAccount(jwt.getSubject());
    }
}
