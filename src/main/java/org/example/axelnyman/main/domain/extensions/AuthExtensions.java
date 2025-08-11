package org.example.axelnyman.main.domain.extensions;

import org.example.axelnyman.main.domain.dtos.UserDtos.AuthResponseDto;
import org.example.axelnyman.main.domain.dtos.UserDtos.UserData;
import org.example.axelnyman.main.domain.model.User;

public final class AuthExtensions {

    private AuthExtensions() {
        // Prevent instantiation
    }


    public static UserData toUserData(User user) {
        return new UserData(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getHousehold().getId(),
                user.getCreatedAt());
    }

    public static AuthResponseDto toAuthResponse(String token, User user) {
        return new AuthResponseDto(token, toUserData(user));
    }
}