package org.example.axelnyman.main.domain.dtos;

public class HouseholdDtos {

    public record HouseholdResponse(
            Long id,
            String name
    ) {}
}