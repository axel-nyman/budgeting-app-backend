package org.example.axelnyman.main.domain.extensions;

import org.example.axelnyman.main.domain.dtos.UserDtos.CreateUserRequest;
import org.example.axelnyman.main.domain.dtos.UserDtos.UserResponse;
import org.example.axelnyman.main.domain.model.User;

public final class DomainExtensions {

    private DomainExtensions() {
        // Prevent instantiation
    }

    public static User toEntity(CreateUserRequest request) {
        return new User(
                request.firstName(),
                request.lastName(),
                request.email());
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail());
    }
}
