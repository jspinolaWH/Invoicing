package com.example.invoicing.common.exception;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicatePriorityException extends RuntimeException {
    public DuplicatePriorityException(String message) { super(message); }
}
