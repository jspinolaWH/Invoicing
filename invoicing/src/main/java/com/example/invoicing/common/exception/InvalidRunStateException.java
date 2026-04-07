package com.example.invoicing.common.exception;

public class InvalidRunStateException extends RuntimeException {
    public InvalidRunStateException(String message) {
        super(message);
    }
}
