package com.example.invoicing.credit;

public class CreditNoteValidationException extends RuntimeException {
    public CreditNoteValidationException(String message) {
        super(message);
    }
}
