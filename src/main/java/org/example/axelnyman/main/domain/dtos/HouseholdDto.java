package org.example.axelnyman.main.domain.dtos;

public class HouseholdDto {

    public record HouseholdResponse(
            Long id,
            String name
    ) {}
}