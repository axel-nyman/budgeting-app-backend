package org.example.axelnyman.main.domain.services;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.example.axelnyman.main.domain.abstracts.IDataService;
import org.example.axelnyman.main.domain.abstracts.IDomainService;
import org.example.axelnyman.main.domain.dtos.UserDtos.*;
import org.example.axelnyman.main.domain.extensions.DomainExtensions;
import org.example.axelnyman.main.domain.model.User;
import org.springframework.stereotype.Service;

@Service
public class DomainService implements IDomainService {

    private final IDataService dataService;

    public DomainService(IDataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public CompletableFuture<UserResponse> createUser(CreateUserRequest request) {
        return dataService.userExistsByEmail(request.email())
                .thenCompose(exists -> {
                    if (exists) {
                        throw new IllegalArgumentException("User with email " + request.email() + " already exists");
                    }

                    User user = DomainExtensions.toEntity(request);
                    return dataService.saveUser(user);
                })
                .thenApply(DomainExtensions::toResponse);
    }

    @Override
    public CompletableFuture<Optional<UserResponse>> getUserById(Long id) {
        return dataService.getUserById(id)
                .thenApply(optionalUser -> optionalUser.map(DomainExtensions::toResponse));
    }

    @Override
    public CompletableFuture<List<UserResponse>> getAllUsers() {
        return dataService.getAllUsers()
                .thenApply(users -> users.stream()
                        .map(DomainExtensions::toResponse)
                        .toList());
    }

    @Override
    public CompletableFuture<Boolean> deleteUser(Long id) {
        return dataService.deleteUserById(id);
    }
}
