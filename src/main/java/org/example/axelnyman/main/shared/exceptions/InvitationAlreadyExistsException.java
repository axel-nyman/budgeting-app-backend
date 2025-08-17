package org.example.axelnyman.main.shared.exceptions;

public class InvitationAlreadyExistsException extends RuntimeException {
    public InvitationAlreadyExistsException(String message) {
        super(message);
    }
}