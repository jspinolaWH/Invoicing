package com.example.invoicing.common.exception;

import com.example.invoicing.run.CustomerLockedException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("STATUS_TRANSITION_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(f -> Map.of("field", f.getField(), "message",
                f.getDefaultMessage() != null ? f.getDefaultMessage() : "Invalid value"))
            .collect(Collectors.toList());
        return ResponseEntity.badRequest()
            .body(Map.of("error", "VALIDATION_ERROR", "fieldErrors", fieldErrors));
    }

    @ExceptionHandler(CustomerLockedException.class)
    public ResponseEntity<ErrorResponse> handleCustomerLocked(CustomerLockedException ex) {
        return ResponseEntity
            .status(HttpStatus.LOCKED)
            .header("Retry-After", "300")
            .body(new ErrorResponse("CUSTOMER_LOCKED",
                "Invoice processing in progress. Address changes cannot be made during this time."));
    }

    @ExceptionHandler(com.example.invoicing.cancellation.CannotCancelException.class)
    public ResponseEntity<ErrorResponse> handleCannotCancel(
            com.example.invoicing.cancellation.CannotCancelException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("CANNOT_CANCEL", ex.getMessage()));
    }

    @ExceptionHandler(com.example.invoicing.run.InvalidRunStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRunState(
            com.example.invoicing.run.InvalidRunStateException ex) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("INVALID_RUN_STATE", ex.getMessage()));
    }

    @ExceptionHandler(com.example.invoicing.credit.CreditNoteValidationException.class)
    public ResponseEntity<ErrorResponse> handleCreditNoteValidation(
            com.example.invoicing.credit.CreditNoteValidationException ex) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("CREDIT_NOTE_VALIDATION_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(com.example.invoicing.recall.CannotRecallException.class)
    public ResponseEntity<ErrorResponse> handleCannotRecall(
            com.example.invoicing.recall.CannotRecallException ex) {
        return ResponseEntity.status(409).body(new ErrorResponse("CANNOT_RECALL", ex.getMessage()));
    }
}
