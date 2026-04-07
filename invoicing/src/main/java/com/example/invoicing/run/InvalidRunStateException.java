package com.example.invoicing.run;

public class InvalidRunStateException extends RuntimeException {
    public InvalidRunStateException(String message) {
        super(message);
    }
}
