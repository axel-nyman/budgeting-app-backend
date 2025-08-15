package org.example.axelnyman.main.infrastructure.data.services;

import java.util.List;
import java.util.Optional;

import org.example.axelnyman.main.domain.abstracts.IDataService;
import org.example.axelnyman.main.domain.model.Household;
import org.example.axelnyman.main.domain.model.User;
import org.example.axelnyman.main.infrastructure.data.context.HouseholdRepository;
import org.example.axelnyman.main.infrastructure.data.context.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class DataService implements IDataService {

    private final UserRepository userRepository;
    private final HouseholdRepository householdRepository;

    public DataService(UserRepository userRepository, HouseholdRepository householdRepository) {
        this.userRepository = userRepository;
        this.householdRepository = householdRepository;
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public boolean deleteUserById(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean userExistsByEmailIncludingDeleted(String email) {
        return userRepository.existsByEmailIncludingDeleted(email);
    }

    @Override
    public Optional<User> findActiveUserByEmail(String email) {
        return userRepository.findActiveByEmail(email);
    }

    @Override
    public Household saveHousehold(Household household) {
        return householdRepository.save(household);
    }

    @Override
    public List<User> getActiveUsersByHouseholdId(Long householdId) {
        return userRepository.findActiveByHouseholdId(householdId);
    }

    @Override
    public Optional<User> getActiveUserByIdAndHouseholdId(Long id, Long householdId) {
        return userRepository.findActiveByIdAndHouseholdId(id, householdId);
    }

    @Override
    public Optional<Household> getHouseholdWithActiveMembers(Long householdId) {
        return householdRepository.findByIdWithActiveMembers(householdId);
    }
}
