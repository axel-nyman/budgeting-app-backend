package org.example.axelnyman.main.domain.services;

import java.util.List;
import java.util.Optional;

import org.example.axelnyman.main.domain.abstracts.IDataService;
import org.example.axelnyman.main.domain.abstracts.IDomainService;
import org.example.axelnyman.main.domain.dtos.UserDto.UserResponse;
import org.example.axelnyman.main.domain.extensions.UserExtensions;
import org.springframework.stereotype.Service;

@Service
public class DomainService implements IDomainService {

    private final IDataService dataService;

    public DomainService(IDataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public Optional<UserResponse> getUserById(Long id) {
        return dataService.getUserById(id)
                .map(UserExtensions::toResponse);
    }

    @Override
    public Optional<UserResponse> getUserByIdInHousehold(Long id, Long householdId) {
        return dataService.getActiveUserByIdAndHouseholdId(id, householdId)
                .map(UserExtensions::toResponse);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return dataService.getAllUsers()
                .stream()
                .map(UserExtensions::toResponse)
                .toList();
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
}
