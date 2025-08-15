package org.example.axelnyman.main.api.endpoints;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.axelnyman.main.domain.abstracts.IDomainService;
import org.example.axelnyman.main.domain.dtos.HouseholdDtos.HouseholdResponse;
import org.example.axelnyman.main.infrastructure.security.CurrentUser;
import org.example.axelnyman.main.infrastructure.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}