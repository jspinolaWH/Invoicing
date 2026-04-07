package com.example.invoicing.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stub HTTP client for the e-invoice operator (e.g. Maventa, Basware).
 * In production this would call the operator's REST/SOAP API.
 */
@Component
@Slf4j
public class EInvoiceOperatorClient {

    public boolean startRegistration(String einvoiceAddress, String operatorCode, String customerName) {
        log.info("OPERATOR START: address={}, operator={}, customer={}", einvoiceAddress, operatorCode, customerName);
        return true;
    }

    public boolean terminateRegistration(String einvoiceAddress, String operatorCode) {
        log.info("OPERATOR TERMINATE: address={}, operator={}", einvoiceAddress, operatorCode);
        return true;
    }
}
