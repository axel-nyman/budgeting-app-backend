package org.example.axelnyman.main.domain.extensions;

import org.example.axelnyman.main.domain.dtos.UserDtos.UserResponse;
import org.example.axelnyman.main.domain.dtos.UserDtos.UserProfileDto;
import org.example.axelnyman.main.domain.model.User;

public final class UserExtensions {

    private UserExtensions() {
        // Prevent instantiation
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail());
    }

    public static UserProfileDto toUserProfileDto(User user) {
        return new UserProfileDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                HouseholdExtensions.toHouseholdDto(user.getHousehold()),
                user.getCreatedAt());
    }
}