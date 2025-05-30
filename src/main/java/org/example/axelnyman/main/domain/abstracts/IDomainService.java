package org.example.axelnyman.main.domain.abstracts;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.example.axelnyman.main.domain.dtos.UserDtos.CreateUserRequest;
import org.example.axelnyman.main.domain.dtos.UserDtos.UserResponse;

public interface IDomainService {
    CompletableFuture<UserResponse> createUser(CreateUserRequest request);

    CompletableFuture<Optional<UserResponse>> getUserById(Long id);

    CompletableFuture<List<UserResponse>> getAllUsers();

    CompletableFuture<Boolean> deleteUser(Long id);
}
