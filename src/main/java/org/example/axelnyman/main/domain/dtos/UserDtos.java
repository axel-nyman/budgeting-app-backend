package org.example.axelnyman.main.domain.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class UserDtos {

    public record RegisterRequest(
            @NotBlank(message = "First name is required") 
            String firstName,
            
            @NotBlank(message = "Last name is required") 
            String lastName,
            
            @Email(message = "Email should be valid") 
            @NotBlank(message = "Email is required") 
            String email,
            
            @NotBlank(message = "Password is required") 
            @Size(min = 8, message = "Password must be at least 8 characters long") 
            String password
    ) {}

    public record LoginRequest(
            @Email(message = "Email should be valid") 
            @NotBlank(message = "Email is required") 
            String email,
            
            @NotBlank(message = "Password is required") 
            String password
    ) {}

    public record UserResponse(
            Long id,
            String firstName,
            String lastName,
            String email,
            HouseholdDtos.HouseholdResponse household,
            LocalDateTime createdAt
    ) {}

    public record AuthResponse(
            String token,
            UserResponse user
    ) {}
}