package com.example.invoicing.recall;

public class CannotRecallException extends RuntimeException {
    public CannotRecallException(String message) {
        super(message);
    }
}
