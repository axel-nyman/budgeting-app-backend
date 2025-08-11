package org.example.axelnyman.main.domain.abstracts;

import org.example.axelnyman.main.domain.dtos.UserDtos.RegisterUserRequest;
import org.example.axelnyman.main.domain.dtos.UserDtos.LoginDto;
import org.example.axelnyman.main.domain.dtos.UserDtos.AuthResponseDto;

/**
 * Authentication Service - Responsible for authentication and user lifecycle operations
 * This service handles user registration, login, password management, and other
 * authentication-specific business logic.
 */
public interface IAuthService {
    AuthResponseDto registerUser(RegisterUserRequest request);
    
    AuthResponseDto login(LoginDto loginDto);
}