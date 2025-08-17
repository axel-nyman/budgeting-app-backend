package org.example.axelnyman.main.domain.extensions;

import org.example.axelnyman.main.domain.dtos.HouseholdDtos.*;
import org.example.axelnyman.main.domain.dtos.UserDtos.UserMemberResponse;
import org.example.axelnyman.main.domain.model.Household;
import org.example.axelnyman.main.domain.model.HouseholdInvitation;
import org.example.axelnyman.main.domain.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class HouseholdExtensions {

    private HouseholdExtensions() {
        // Prevent instantiation
    }

    public static SimpleHouseholdResponse toSimpleResponse(Household household) {
        return new SimpleHouseholdResponse(
                household.getId(),
                household.getName());
    }

    public static HouseholdResponse toResponse(Household household) {
        List<User> activeUsers = household.getUsers().stream()
                .filter(user -> user.getDeletedAt() == null)
                .collect(Collectors.toList());
        
        List<UserMemberResponse> members = activeUsers.stream()
                .map(UserExtensions::toMemberResponse)
                .collect(Collectors.toList());

        return new HouseholdResponse(
                household.getId(),
                household.getName(),
                household.getCreatedAt(),
                members,
                activeUsers.size());
    }

    public static HouseholdUpdateResponse toUpdateResponse(Household household) {
        return new HouseholdUpdateResponse(
                household.getId(),
                household.getName(),
                household.getCreatedAt(),
                household.getUpdatedAt());
    }

    public static Household toEntity(String name) {
        return new Household(name);
    }

    public static HouseholdInvitation toInvitationEntity(Household household, User invitedUser, User invitedByUser) {
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        return new HouseholdInvitation(household, invitedUser, invitedByUser, token, expiresAt);
    }

    public static InvitationResponse toInvitationResponse(HouseholdInvitation invitation) {
        UserMemberResponse inviterDetails = UserExtensions.toMemberResponse(invitation.getInvitedByUser());

        return new InvitationResponse(
                invitation.getId(),
                invitation.getHousehold().getId(),
                invitation.getHousehold().getName(),
                invitation.getInvitedUser().getEmail(),
                inviterDetails,
                invitation.getExpiresAt(),
                invitation.getStatus().toString()
        );
    }
}