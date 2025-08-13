package org.example.axelnyman.main.domain.abstracts;

import java.util.List;
import java.util.Optional;

import org.example.axelnyman.main.domain.dtos.UserDto.UserResponse;

/**
 * Domain Service - Responsible for general business operations
 * This service handles CRUD operations, data transformations, and business rules
 * that apply across the application domain.
 */
public interface IDomainService {
    Optional<UserResponse> getUserById(Long id);

    Optional<UserResponse> getUserByIdInHousehold(Long id, Long householdId);

    List<UserResponse> getAllUsers();

    boolean deleteUser(Long id);

    Optional<UserResponse> getUserProfile(Long userId);

    List<UserResponse> getHouseholdUsers(Long householdId);
}
