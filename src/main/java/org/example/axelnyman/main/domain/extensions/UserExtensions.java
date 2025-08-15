package org.example.axelnyman.main.domain.extensions;

import org.example.axelnyman.main.domain.dtos.UserDtos.*;
import org.example.axelnyman.main.domain.model.User;
import org.example.axelnyman.main.domain.model.Household;

public final class UserExtensions {

    private UserExtensions() {
        // Prevent instantiation
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                HouseholdExtensions.toSimpleResponse(user.getHousehold()),
                user.getCreatedAt());
    }

    public static AuthResponse toAuthResponse(String token, User user) {
        return new AuthResponse(token, toResponse(user));
    }

    public static UserMemberResponse toMemberResponse(User user) {
        return new UserMemberResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getCreatedAt());
    }

    public static User toEntity(RegisterRequest request, Household household) {
        return new User(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.password(),
                household);
    }
}