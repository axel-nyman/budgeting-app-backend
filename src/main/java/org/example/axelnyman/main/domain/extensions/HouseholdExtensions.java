package org.example.axelnyman.main.domain.extensions;

import org.example.axelnyman.main.domain.dtos.HouseholdDtos.*;
import org.example.axelnyman.main.domain.model.Household;

public final class HouseholdExtensions {

    private HouseholdExtensions() {
        // Prevent instantiation
    }

    public static HouseholdResponse toResponse(Household household) {
        return new HouseholdResponse(
                household.getId(),
                household.getName());
    }
}