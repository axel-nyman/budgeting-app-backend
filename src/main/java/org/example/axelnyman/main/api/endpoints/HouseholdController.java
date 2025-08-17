package org.example.axelnyman.main.api.endpoints;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.axelnyman.main.domain.abstracts.IDomainService;
import org.example.axelnyman.main.domain.dtos.HouseholdDtos.CreateInvitationRequest;
import org.example.axelnyman.main.domain.dtos.HouseholdDtos.HouseholdResponse;
import org.example.axelnyman.main.domain.dtos.HouseholdDtos.HouseholdUpdateResponse;
import org.example.axelnyman.main.domain.dtos.HouseholdDtos.InvitationResponse;
import org.example.axelnyman.main.domain.dtos.HouseholdDtos.UpdateHouseholdRequest;
import org.example.axelnyman.main.infrastructure.security.CurrentUser;
import org.example.axelnyman.main.infrastructure.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
@Tag(name = "Households", description = "Household management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class HouseholdController {

    private final IDomainService domainService;

    public HouseholdController(IDomainService domainService) {
        this.domainService = domainService;
    }

    @GetMapping("/households")
    @Operation(summary = "Get household information", description = "Retrieve household information and active members for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Household information retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Household not found")
    })
    public ResponseEntity<HouseholdResponse> getHousehold(@CurrentUser UserPrincipal currentUser) {
        return domainService.getHouseholdDetails(currentUser.getHouseholdId())
                .map(household -> ResponseEntity.ok(household))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/households")
    @Operation(summary = "Update household name", description = "Rename the household for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Household name updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid household name"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Household not found")
    })
    public ResponseEntity<HouseholdUpdateResponse> updateHouseholdName(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody UpdateHouseholdRequest request) {
        HouseholdUpdateResponse response = domainService.updateHouseholdName(
                currentUser.getHouseholdId(), 
                request.name());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/households/invitations")
    @Operation(summary = "Create household invitation", description = "Invite another user to join the household by email address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Invitation created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or user already in household"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "User with this email not found")
    })
    public ResponseEntity<InvitationResponse> createInvitation(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody CreateInvitationRequest request) {
        InvitationResponse response = domainService.createHouseholdInvitation(
                currentUser.getHouseholdId(),
                currentUser.getUserId(),
                request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}