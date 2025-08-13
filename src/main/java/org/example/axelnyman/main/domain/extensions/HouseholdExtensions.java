package org.example.axelnyman.main.domain.extensions;

import org.example.axelnyman.main.domain.dtos.HouseholdDtos.HouseholdDto;
import org.example.axelnyman.main.domain.model.Household;

public final class HouseholdExtensions {

    private HouseholdExtensions() {
        // Prevent instantiation
    }

    public static HouseholdDto toHouseholdDto(Household household) {
        return new HouseholdDto(
                household.getId(),
                household.getName());
    }
}