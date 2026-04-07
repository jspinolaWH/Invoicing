package com.example.invoicing.common.exception;

public class CannotCancelException extends RuntimeException {
    public CannotCancelException(String message) {
        super(message);
    }
}
