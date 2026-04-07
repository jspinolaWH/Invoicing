package com.example.invoicing.accounting.vat;

public class VatRateNotFoundException extends RuntimeException {
    public VatRateNotFoundException(String eventDate) {
        super("No VAT rate found for event date: " + eventDate);
    }
}
