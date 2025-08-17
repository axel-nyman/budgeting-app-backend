package org.example.axelnyman.main.shared.exceptions;

public class UserAlreadyInHouseholdException extends RuntimeException {
    public UserAlreadyInHouseholdException(String message) {
        super(message);
    }
}