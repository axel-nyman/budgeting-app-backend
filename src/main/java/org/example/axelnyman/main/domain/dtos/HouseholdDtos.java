package org.example.axelnyman.main.domain.dtos;

import org.example.axelnyman.main.domain.dtos.UserDtos.UserMemberResponse;

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
}