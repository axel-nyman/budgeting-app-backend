package org.example.axelnyman.main.api.endpoints;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.axelnyman.main.domain.abstracts.IDomainService;
import org.example.axelnyman.main.domain.dtos.UserDtos.*;
import org.example.axelnyman.main.domain.dtos.HouseholdDtos.*;
import org.example.axelnyman.main.infrastructure.security.CurrentUser;
import org.example.axelnyman.main.infrastructure.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Users", description = "User management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final IDomainService domainService;

    public UserController(IDomainService domainService) {
        this.domainService = domainService;
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID from the same household")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id, @CurrentUser UserPrincipal currentUser) {
        return domainService.getUserByIdInHousehold(id, currentUser.getHouseholdId())
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/users")
    @Operation(summary = "Get household users", description = "Retrieve all active users in the authenticated user's household")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Household users retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<List<UserResponse>> getHouseholdUsers(@CurrentUser UserPrincipal currentUser) {
        List<UserResponse> users = domainService.getHouseholdUsers(currentUser.getHouseholdId());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/me")
    @Operation(summary = "Get current user profile", description = "Get the currently authenticated user's profile information including household details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Current user profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> getCurrentUser(@CurrentUser UserPrincipal currentUser) {
        return domainService.getUserProfile(currentUser.getUserId())
                .map(profile -> ResponseEntity.ok(profile))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete user", description = "Delete a user by their ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        boolean deleted = domainService.deleteUser(id);
        return deleted
                ? ResponseEntity.noContent().<Void>build()
                : ResponseEntity.notFound().<Void>build();
    }

    @GetMapping("/users/me/invitations")
    @Operation(summary = "Get user's pending invitations", description = "Retrieve all pending household invitations for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pending invitations retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<List<InvitationResponse>> getUserInvitations(@CurrentUser UserPrincipal currentUser) {
        List<InvitationResponse> invitations = domainService.getUserPendingInvitations(currentUser.getUserId());
        return ResponseEntity.ok(invitations);
    }
}
