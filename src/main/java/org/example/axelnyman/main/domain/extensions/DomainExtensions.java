package org.example.axelnyman.main.domain.extensions;

import org.example.axelnyman.main.domain.dtos.UserDtos.UserResponse;
import org.example.axelnyman.main.domain.dtos.UserDtos.UserProfileDto;
import org.example.axelnyman.main.domain.dtos.UserDtos.HouseholdDto;
import org.example.axelnyman.main.domain.model.User;
import org.example.axelnyman.main.domain.model.Household;

public final class DomainExtensions {

    private DomainExtensions() {
        // Prevent instantiation
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail());
    }

    public static HouseholdDto toHouseholdDto(Household household) {
        return new HouseholdDto(
                household.getId(),
                household.getName());
    }

    public static UserProfileDto toUserProfileDto(User user) {
        return new UserProfileDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                toHouseholdDto(user.getHousehold()),
                user.getCreatedAt());
    }
}
