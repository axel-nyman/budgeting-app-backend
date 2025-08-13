package org.example.axelnyman.main.domain.abstracts;

import java.util.List;
import java.util.Optional;

import org.example.axelnyman.main.domain.model.Household;
import org.example.axelnyman.main.domain.model.User;

/**
 * Data Access Service - Responsible for direct database operations
 * This service provides a clean abstraction over repository operations
 * and should not contain business logic.
 */
public interface IDataService {
    User saveUser(User user);

    Optional<User> getUserById(Long id);

    List<User> getAllUsers();

    boolean deleteUserById(Long id);

    boolean userExistsByEmail(String email);

    boolean userExistsByEmailIncludingDeleted(String email);

    Optional<User> findActiveUserByEmail(String email);

    Household saveHousehold(Household household);

    Optional<Household> getHouseholdById(Long id);

    List<User> getActiveUsersByHouseholdId(Long householdId);

    Optional<User> getActiveUserByIdAndHouseholdId(Long id, Long householdId);
}
