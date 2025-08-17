package org.example.axelnyman.main.shared.exceptions;

public class HouseholdNotFoundException extends RuntimeException {

    public HouseholdNotFoundException(String message) {
        super(message);
    }
}