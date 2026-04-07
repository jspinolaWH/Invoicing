package com.example.invoicing.cancellation;

public class CannotCancelException extends RuntimeException {
    public CannotCancelException(String message) {
        super(message);
    }
}
