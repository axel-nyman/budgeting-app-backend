package org.example.axelnyman.main.domain.dtos;

import org.example.axelnyman.main.domain.dtos.UserDtos.UserMemberResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public class HouseholdDtos {

    public record SimpleHouseholdResponse(
            Long id,
            String name
    ) {}

    public record HouseholdResponse(
            Long id,
            String name,
            LocalDateTime createdAt,
            List<UserMemberResponse> members,
            Integer memberCount
    ) {}

    public record UpdateHouseholdRequest(
            @NotBlank(message = "Name cannot be empty")
            @Size(max = 100, message = "Name cannot exceed 100 characters")
            String name
    ) {}

    public record HouseholdUpdateResponse(
            Long id,
            String name,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record CreateInvitationRequest(
            @NotBlank(message = "Email is required")
            @jakarta.validation.constraints.Email(message = "Email should be valid")
            String email
    ) {}

    public record InvitationResponse(
            Long id,
            Long householdId,
            String householdName,
            String invitedEmail,
            UserMemberResponse invitedBy,
            LocalDateTime expiresAt,
            String status
    ) {}
}