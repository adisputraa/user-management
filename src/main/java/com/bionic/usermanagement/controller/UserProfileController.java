package com.bionic.usermanagement.controller;

import com.bionic.usermanagement.dto.UpdateProfileRequest;
import com.bionic.usermanagement.dto.UserProfileDto;
import com.bionic.usermanagement.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profiles")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        UserProfileDto profile = userProfileService.getMyProfile(jwt);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDto> updateMyProfile(@AuthenticationPrincipal Jwt jwt,
                                                          @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileDto updatedProfile = userProfileService.updateMyProfile(jwt, request);
        return ResponseEntity.ok(updatedProfile);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyProfile(@AuthenticationPrincipal Jwt jwt) {
        userProfileService.deleteMyProfile(jwt);
        return ResponseEntity.noContent().build();
    }
}
