package org.example.axelnyman.main.api.endpoints;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.axelnyman.main.domain.abstracts.IAuthService;
import org.example.axelnyman.main.domain.dtos.UserDtos.RegisterUserRequest;
import org.example.axelnyman.main.shared.exceptions.DuplicateEmailException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

    private final IAuthService authService;

    public AuthController(IAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Register a new user with firstName, lastName, email, and password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or duplicate email")
    })
    public CompletableFuture<ResponseEntity<Object>> register(@Valid @RequestBody RegisterUserRequest request) {
        return authService.registerUser(request)
                .thenApply(response -> ResponseEntity.status(HttpStatus.CREATED).body((Object) response))
                .exceptionally(throwable -> {
                    if (throwable.getCause() instanceof DuplicateEmailException) {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("error", throwable.getCause().getMessage());
                        Map<String, String[]> details = new HashMap<>();
                        details.put("email", new String[] { "Email already exists" });
                        errorResponse.put("details", details);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) errorResponse);
                    }
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("error", "An unexpected error occurred");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) errorResponse);
                });
    }
}