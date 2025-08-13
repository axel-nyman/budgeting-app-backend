package org.example.axelnyman.main.domain.dtos;

import java.util.List;

public class HouseholdDtos {
    public record HouseholdDto(
            Long id,
            String name) {
    }

    public record HouseholdUsersResponse(
            List<UserDtos.UserResponse> users) {
    }
}