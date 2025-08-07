package org.example.axelnyman.main.infrastructure.data.services;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.example.axelnyman.main.domain.abstracts.IDataService;
import org.example.axelnyman.main.domain.model.Household;
import org.example.axelnyman.main.domain.model.User;
import org.example.axelnyman.main.infrastructure.data.context.HouseholdRepository;
import org.example.axelnyman.main.infrastructure.data.context.UserRepository;
import org.springframework.scheduling.annotation.Async;
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
    @Async
    public CompletableFuture<User> saveUser(User user) {
        return CompletableFuture.completedFuture(userRepository.save(user));
    }

    @Override
    @Async
    public CompletableFuture<Optional<User>> getUserById(Long id) {
        return CompletableFuture.completedFuture(userRepository.findById(id));
    }

    @Override
    @Async
    public CompletableFuture<List<User>> getAllUsers() {
        return CompletableFuture.completedFuture(userRepository.findAll());
    }

    @Override
    @Async
    public CompletableFuture<Boolean> userExistsByEmail(String email) {
        return CompletableFuture.completedFuture(userRepository.existsByEmail(email));
    }

    @Override
    @Async
    public CompletableFuture<Boolean> deleteUserById(Long id) {
        return CompletableFuture.supplyAsync(() -> {
            if (userRepository.existsById(id)) {
                userRepository.deleteById(id);
                return true;
            }
            return false;
        });
    }

    @Override
    @Async
    public CompletableFuture<Boolean> userExistsByEmailIncludingDeleted(String email) {
        return CompletableFuture.completedFuture(userRepository.existsByEmailIncludingDeleted(email));
    }

    @Override
    @Async
    public CompletableFuture<Household> saveHousehold(Household household) {
        return CompletableFuture.completedFuture(householdRepository.save(household));
    }

    @Override
    @Async
    public CompletableFuture<Optional<Household>> getHouseholdById(Long id) {
        return CompletableFuture.completedFuture(householdRepository.findById(id));
    }
}
