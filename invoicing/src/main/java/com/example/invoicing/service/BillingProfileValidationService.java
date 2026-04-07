package com.example.invoicing.service;
import com.example.invoicing.entity.customer.*;
import com.example.invoicing.entity.validation.*;
import com.example.invoicing.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillingProfileValidationService {
    private final CustomerBillingProfileRepository customerRepo;
    private final EInvoiceAddressRepository einvoiceAddressRepo;
    private final ValidationRuleRepository validationRuleRepo;

    public ValidationReport validate(Long customerId, Long companyId) {
        Customer customer = customerRepo.findById(customerId)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + customerId));
        List<ValidationFailure> failures = new ArrayList<>();
        BillingProfile profile = customer.getBillingProfile();

        // 1. CustomerID: 6-9 digits
        if (profile == null || profile.getCustomerIdNumber() == null
                || !profile.getCustomerIdNumber().matches("\\d{6,9}")) {
            failures.add(failure(customerId, "billingProfile.customerIdNumber",
                "CUSTOMER_ID_FORMAT", ValidationSeverity.BLOCKING,
                "Customer ID must be a 6-9 digit numeric sequence (PD-298)"));
        }
        // 2. Delivery method
        if (profile == null || profile.getDeliveryMethod() == null) {
            failures.add(failure(customerId, "billingProfile.deliveryMethod",
                "DELIVERY_METHOD_MISSING", ValidationSeverity.BLOCKING,
                "Invoice delivery method is required (PD-298)"));
        }
        // 3. Billing address fields
        BillingAddress addr = profile != null ? profile.getBillingAddress() : null;
        if (addr == null || isBlank(addr.getStreetAddress()))
            failures.add(failure(customerId, "billingProfile.billingAddress.streetAddress",
                "BILLING_ADDRESS_INCOMPLETE", ValidationSeverity.BLOCKING,
                "Billing address street is required (PD-298)"));
        if (addr == null || isBlank(addr.getPostalCode()))
            failures.add(failure(customerId, "billingProfile.billingAddress.postalCode",
                "BILLING_ADDRESS_INCOMPLETE", ValidationSeverity.BLOCKING,
                "Billing address postal code is required (PD-298)"));
        if (addr == null || isBlank(addr.getCity()))
            failures.add(failure(customerId, "billingProfile.billingAddress.city",
                "BILLING_ADDRESS_INCOMPLETE", ValidationSeverity.BLOCKING,
                "Billing address city is required (PD-298)"));
        // 4. Business ID for E_INVOICE
        if (profile != null && profile.getDeliveryMethod() == DeliveryMethod.E_INVOICE
                && isBlank(profile.getBusinessId()))
            failures.add(failure(customerId, "billingProfile.businessId",
                "BUSINESS_ID_REQUIRED_FOR_EINVOICE", ValidationSeverity.BLOCKING,
                "Business ID is required for e-invoice delivery (PD-298)"));
        // 5. EInvoice address for E_INVOICE
        if (profile != null && profile.getDeliveryMethod() == DeliveryMethod.E_INVOICE) {
            boolean hasAddress = einvoiceAddressRepo.findByCustomer_Id(customerId)
                .map(e -> !isBlank(e.getAddress())).orElse(false);
            if (!hasAddress)
                failures.add(failure(customerId, "einvoiceAddress.address",
                    "EINVOICE_ADDRESS_MISSING", ValidationSeverity.BLOCKING,
                    "An e-invoice address is required when delivery method is E_INVOICE (PD-282)"));
        }
        // 6. Language code - WARNING
        if (profile != null && isBlank(profile.getLanguageCode()))
            failures.add(failure(customerId, "billingProfile.languageCode",
                "LANGUAGE_CODE_MISSING", ValidationSeverity.WARNING,
                "Language code is not set. Finnish (fi) will be used as default (PD-308)"));

        // Company-configured MANDATORY_FIELD rules
        validationRuleRepo.findAllActiveByCompanyId(companyId).stream()
            .filter(r -> r.getRuleType() == ValidationRuleType.MANDATORY_FIELD)
            .forEach(rule -> evaluateMandatoryFieldRule(rule, customerId, profile, failures));

        long blockingCount = failures.stream()
            .filter(f -> f.getSeverity() == ValidationSeverity.BLOCKING).count();
        return ValidationReport.builder()
            .totalChecked(1).passed(blockingCount == 0 ? 1 : 0).failures(failures).build();
    }

    public ValidationReport validateAll(List<Long> customerIds, Long companyId) {
        List<ValidationFailure> all = new ArrayList<>();
        int passed = 0;
        for (Long id : customerIds) {
            ValidationReport r = validate(id, companyId);
            all.addAll(r.getFailures()); passed += r.getPassed();
        }
        return ValidationReport.builder().totalChecked(customerIds.size()).passed(passed).failures(all).build();
    }

    private void evaluateMandatoryFieldRule(ValidationRule rule, Long customerId,
                                             BillingProfile profile, List<ValidationFailure> failures) {
        try {
            String fieldPath = extractFieldFromConfig(rule.getConfig());
            if (!resolveFieldPresence(profile, fieldPath))
                failures.add(failure(customerId, fieldPath, rule.getRuleCode(),
                    rule.isBlocking() ? ValidationSeverity.BLOCKING : ValidationSeverity.WARNING,
                    rule.getDescription()));
        } catch (Exception ignored) {}
    }

    private ValidationFailure failure(Long customerId, String field, String rule,
                                       ValidationSeverity severity, String description) {
        return ValidationFailure.builder().entityId(customerId).entityType("CUSTOMER")
            .field(field).rule(rule).severity(severity).description(description).build();
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    private boolean resolveFieldPresence(BillingProfile profile, String fieldPath) {
        if (profile == null) return false;
        return switch (fieldPath) {
            case "billingProfile.businessId"    -> !isBlank(profile.getBusinessId());
            case "billingProfile.languageCode"  -> !isBlank(profile.getLanguageCode());
            case "billingProfile.invoicingMode" -> profile.getInvoicingMode() != null;
            default -> true;
        };
    }

    private String extractFieldFromConfig(String config) {
        int start = config.indexOf("\"field\":\"") + 9;
        int end = config.indexOf("\"", start);
        return config.substring(start, end);
    }
}
