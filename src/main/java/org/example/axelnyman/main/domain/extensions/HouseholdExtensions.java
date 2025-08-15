package org.example.axelnyman.main.domain.extensions;

import org.example.axelnyman.main.domain.dtos.HouseholdDtos.*;
import org.example.axelnyman.main.domain.dtos.UserDtos.UserMemberResponse;
import org.example.axelnyman.main.domain.model.Household;
import org.example.axelnyman.main.domain.model.User;

import java.util.List;
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

    public static Household toEntity(String name) {
        return new Household(name);
    }
}