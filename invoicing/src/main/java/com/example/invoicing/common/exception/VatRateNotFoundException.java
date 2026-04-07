package com.example.invoicing.common.exception;

public class VatRateNotFoundException extends RuntimeException {
    public VatRateNotFoundException(String eventDate) {
        super("No VAT rate found for event date: " + eventDate);
    }
}
