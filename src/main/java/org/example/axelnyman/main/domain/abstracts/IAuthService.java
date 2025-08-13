package org.example.axelnyman.main.domain.abstracts;

import org.example.axelnyman.main.domain.dtos.UserDto.RegisterRequest;
import org.example.axelnyman.main.domain.dtos.UserDto.LoginRequest;
import org.example.axelnyman.main.domain.dtos.UserDto.AuthResponse;

/**
 * Authentication Service - Responsible for authentication and user lifecycle operations
 * This service handles user registration, login, password management, and other
 * authentication-specific business logic.
 */
public interface IAuthService {
    AuthResponse registerUser(RegisterRequest request);
    
    AuthResponse login(LoginRequest loginRequest);
}