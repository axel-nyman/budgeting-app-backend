package org.example.axelnyman.main.domain.services;

import java.util.List;
import java.util.Optional;

import org.example.axelnyman.main.domain.abstracts.IDataService;
import org.example.axelnyman.main.domain.abstracts.IDomainService;
import org.example.axelnyman.main.domain.dtos.UserDtos.*;
import org.example.axelnyman.main.domain.dtos.HouseholdDtos.*;
import org.example.axelnyman.main.domain.extensions.UserExtensions;
import org.example.axelnyman.main.domain.extensions.HouseholdExtensions;
import org.springframework.stereotype.Service;

@Service
public class DomainService implements IDomainService {

    private final IDataService dataService;

    public DomainService(IDataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public Optional<UserResponse> getUserByIdInHousehold(Long id, Long householdId) {
        return dataService.getActiveUserByIdAndHouseholdId(id, householdId)
                .map(UserExtensions::toResponse);
    }

    @Override
    public boolean deleteUser(Long id) {
        return dataService.deleteUserById(id);
    }

    @Override
    public Optional<UserResponse> getUserProfile(Long userId) {
        return dataService.getUserById(userId)
                .map(UserExtensions::toResponse);
    }

    @Override
    public List<UserResponse> getHouseholdUsers(Long householdId) {
        return dataService.getActiveUsersByHouseholdId(householdId)
                .stream()
                .map(UserExtensions::toResponse)
                .toList();
    }

    @Override
    public Optional<HouseholdResponse> getHouseholdDetails(Long householdId) {
        return dataService.getHouseholdWithActiveMembers(householdId)
                .map(HouseholdExtensions::toResponse);
    }
}
