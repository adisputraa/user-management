package com.bionic.usermanagement.controller;

import com.bionic.usermanagement.dto.KeycloakTokenResponse;
import com.bionic.usermanagement.dto.LoginRequest;
import com.bionic.usermanagement.dto.RefreshTokenRequest;
import com.bionic.usermanagement.dto.RegisterRequest;
import com.bionic.usermanagement.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<KeycloakTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        KeycloakTokenResponse tokenResponse = authService.login(request);
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<KeycloakTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        KeycloakTokenResponse tokenResponse = authService.refreshToken(request);
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok().build();
    }
}
