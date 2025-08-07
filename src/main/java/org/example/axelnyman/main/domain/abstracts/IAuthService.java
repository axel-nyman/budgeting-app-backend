package org.example.axelnyman.main.domain.abstracts;

import org.example.axelnyman.main.domain.dtos.UserDtos.RegisterUserRequest;
import org.example.axelnyman.main.domain.dtos.UserDtos.UserRegistrationResponse;

import java.util.concurrent.CompletableFuture;

public interface IAuthService {
    CompletableFuture<UserRegistrationResponse> registerUser(RegisterUserRequest request);
}