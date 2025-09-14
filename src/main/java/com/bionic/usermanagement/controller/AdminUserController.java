package com.bionic.usermanagement.controller;

import com.bionic.usermanagement.dto.UpdateUserRequest;
import com.bionic.usermanagement.dto.UpdateUserStatusRequest;
import com.bionic.usermanagement.dto.UserProfileDto;
import com.bionic.usermanagement.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<Page<UserProfileDto>> getAllUsers(@PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        Page<UserProfileDto> users = adminUserService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileDto> getUserById(@PathVariable UUID id) {
        UserProfileDto user = adminUserService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateUserStatus(@PathVariable UUID id, @Valid @RequestBody UpdateUserStatusRequest request) {
        adminUserService.updateUserStatus(id, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        adminUserService.updateUser(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        adminUserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}