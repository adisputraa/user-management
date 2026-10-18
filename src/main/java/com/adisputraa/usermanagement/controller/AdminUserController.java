package com.adisputraa.usermanagement.controller;

import com.adisputraa.usermanagement.dto.UpdateUserRoleRequest;
import com.adisputraa.usermanagement.dto.UpdateUserStatusRequest;
import com.adisputraa.usermanagement.dto.UserResponse;
import com.adisputraa.usermanagement.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Requires the ADMIN realm role (enforced in SecurityConfig for /admin/**). */
@RestController
@RequestMapping("/admin/users")
@Tag(name = "User administration", description = "ADMIN role only")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(summary = "List users, newest first (supports page, size, sort)")
    public Page<UserResponse> getUsers(
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return adminUserService.getUsers(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user by ID")
    public UserResponse getUser(@PathVariable UUID id) {
        return adminUserService.getUser(id);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a user (also enables/disables login in Keycloak)")
    public UserResponse updateStatus(@PathVariable UUID id,
                                     @Valid @RequestBody UpdateUserStatusRequest request,
                                     @AuthenticationPrincipal Jwt admin) {
        return adminUserService.updateStatus(id, request.status(), admin.getSubject());
    }

    @PutMapping("/{id}/role")
    @Operation(summary = "Change a user's role in the database and in Keycloak")
    public UserResponse updateRole(@PathVariable UUID id,
                                   @Valid @RequestBody UpdateUserRoleRequest request,
                                   @AuthenticationPrincipal Jwt admin) {
        return adminUserService.updateRole(id, request.role(), admin.getSubject());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a user from Keycloak and the database")
    public void deleteUser(@PathVariable UUID id, @AuthenticationPrincipal Jwt admin) {
        adminUserService.deleteUser(id, admin.getSubject());
    }
}
