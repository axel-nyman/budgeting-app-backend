package org.example.axelnyman.main.domain.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UserDtos {
        public record CreateUserRequest(
                        @NotBlank(message = "First name is required") String firstName,

                        @NotBlank(message = "Last name is required") String lastName,

                        @Email(message = "Email should be valid") @NotBlank(message = "Email is required") String email) {
        }

        public record UserResponse(
                        Long id,
                        String firstName,
                        String lastName,
                        String email) {
        }
}
