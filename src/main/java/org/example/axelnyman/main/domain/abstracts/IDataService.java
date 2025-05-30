package org.example.axelnyman.main.domain.abstracts;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.example.axelnyman.main.domain.model.User;

public interface IDataService {
    CompletableFuture<User> saveUser(User user);

    CompletableFuture<Optional<User>> getUserById(Long id);

    CompletableFuture<List<User>> getAllUsers();

    CompletableFuture<Boolean> deleteUserById(Long id);

    CompletableFuture<Boolean> userExistsByEmail(String email);
}
