package org.example.axelnyman.main.domain.services;

import org.example.axelnyman.main.domain.abstracts.IAuthService;
import org.example.axelnyman.main.domain.abstracts.IDataService;
import org.example.axelnyman.main.domain.dtos.UserDtos.RegisterUserRequest;
import org.example.axelnyman.main.domain.dtos.UserDtos.UserRegistrationResponse;
import org.example.axelnyman.main.domain.dtos.UserDtos.UserRegistrationData;
import org.example.axelnyman.main.domain.model.Household;
import org.example.axelnyman.main.domain.model.User;
import org.example.axelnyman.main.shared.exceptions.DuplicateEmailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements IAuthService {

    private final IDataService dataService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(IDataService dataService, PasswordEncoder passwordEncoder) {
        this.dataService = dataService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserRegistrationResponse registerUser(RegisterUserRequest request) {
        boolean exists = dataService.userExistsByEmailIncludingDeleted(request.email());
        if (exists) {
            throw new DuplicateEmailException("User with email " + request.email() + " already exists");
        }

        // Create household first
        String householdName = request.firstName() + " " + request.lastName() + "'s Household";
        Household household = new Household(householdName);
        Household savedHousehold = dataService.saveHousehold(household);

        // Create user with hashed password and household reference
        String hashedPassword = passwordEncoder.encode(request.password());
        User user = new User(
                request.firstName(),
                request.lastName(),
                request.email(),
                hashedPassword,
                savedHousehold
        );

        User savedUser = dataService.saveUser(user);
        
        return new UserRegistrationResponse(
                "User registered successfully",
                new UserRegistrationData(
                        savedUser.getId(),
                        savedUser.getFirstName(),
                        savedUser.getLastName(),
                        savedUser.getEmail(),
                        savedUser.getHousehold().getId(),
                        savedUser.getCreatedAt()
                )
        );
    }
}