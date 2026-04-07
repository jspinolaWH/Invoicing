package com.example.invoicing.service;

import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.validation.*;
import com.example.invoicing.repository.BillingEventRepository;
import com.example.invoicing.repository.ValidationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingEventValidationService {

    private final BillingEventRepository billingEventRepository;
    private final ValidationRuleRepository validationRuleRepository;

    public ValidationReport validate(List<Long> eventIds) {
        List<BillingEvent> events = billingEventRepository.findAllById(eventIds);
        List<ValidationRule> activeRules = validationRuleRepository.findAllActive();

        List<ValidationFailure> failures = new ArrayList<>();
        int passed = 0;

        for (BillingEvent event : events) {
            List<ValidationFailure> eventFailures = validateEvent(event, activeRules);
            if (eventFailures.isEmpty()) {
                passed++;
            } else {
                failures.addAll(eventFailures);
            }
        }

        long blocking = failures.stream().filter(f -> f.getSeverity() == ValidationSeverity.BLOCKING).count();
        long warnings = failures.stream().filter(f -> f.getSeverity() == ValidationSeverity.WARNING).count();

        return ValidationReport.builder()
            .totalChecked(events.size())
            .passed(passed)
            .blockingFailureCount((int) blocking)
            .warningCount((int) warnings)
            .failures(failures)
            .build();
    }

    public List<ValidationFailure> validateEvent(BillingEvent event, List<ValidationRule> rules) {
        List<ValidationFailure> failures = new ArrayList<>();
        for (ValidationRule rule : rules) {
            switch (rule.getRuleType()) {
                case MANDATORY_FIELD    -> checkMandatoryField(event, rule, failures);
                case PRICE_CONSISTENCY  -> checkPriceConsistency(event, rule, failures);
                case QUANTITY_THRESHOLD -> checkQuantityThreshold(event, rule, failures);
                case CLASSIFICATION     -> checkClassification(event, rule, failures);
            }
        }
        return failures;
    }

    // -----------------------------------------------------------------------
    private void checkMandatoryField(BillingEvent event, ValidationRule rule,
                                      List<ValidationFailure> failures) {
        String fieldName = configValue(rule, "field");
        if (fieldName == null) return;

        boolean missing = switch (fieldName) {
            case "product"             -> event.getProduct() == null;
            case "eventDate"           -> event.getEventDate() == null;
            case "customerNumber"      -> event.getCustomerNumber() == null || event.getCustomerNumber().isBlank();
            case "wasteFeePrice"       -> event.getWasteFeePrice() == null;
            case "transportFeePrice"   -> event.getTransportFeePrice() == null;
            case "ecoFeePrice"         -> event.getEcoFeePrice() == null;
            case "accountingAccount"   -> event.getAccountingAccount() == null;
            case "costCenter"          -> event.getCostCenter() == null;
            case "legalClassification" -> event.getLegalClassification() == null;
            default -> false;
        };

        if (missing) {
            failures.add(ValidationFailure.builder()
                .entityId(event.getId())
                .entityType("BILLING_EVENT")
                .ruleCode("MISSING_" + fieldName.toUpperCase())
                .rule("MANDATORY_FIELD")
                .field(fieldName)
                .severity(rule.isBlocking() ? ValidationSeverity.BLOCKING : ValidationSeverity.WARNING)
                .description("Field '" + fieldName + "' is required but missing on event " + event.getId())
                .build());
        }
    }

    private void checkPriceConsistency(BillingEvent event, ValidationRule rule,
                                        List<ValidationFailure> failures) {
        if (event.isNonBillable()) return;

        List<String> negativeFields = new ArrayList<>();
        if (event.getWasteFeePrice() != null && event.getWasteFeePrice().compareTo(BigDecimal.ZERO) < 0)
            negativeFields.add("wasteFeePrice");
        if (event.getTransportFeePrice() != null && event.getTransportFeePrice().compareTo(BigDecimal.ZERO) < 0)
            negativeFields.add("transportFeePrice");
        if (event.getEcoFeePrice() != null && event.getEcoFeePrice().compareTo(BigDecimal.ZERO) < 0)
            negativeFields.add("ecoFeePrice");

        for (String field : negativeFields) {
            failures.add(ValidationFailure.builder()
                .entityId(event.getId())
                .entityType("BILLING_EVENT")
                .ruleCode("NEGATIVE_PRICE")
                .rule("PRICE_CONSISTENCY")
                .field(field)
                .severity(rule.isBlocking() ? ValidationSeverity.BLOCKING : ValidationSeverity.WARNING)
                .description("Price field '" + field + "' is negative on event " + event.getId()
                    + ". Negative prices are only valid for credit events.")
                .build());
        }
    }

    private void checkQuantityThreshold(BillingEvent event, ValidationRule rule,
                                         List<ValidationFailure> failures) {
        String field = configValue(rule, "field");
        String thresholdStr = configValue(rule, "threshold");
        if (field == null || thresholdStr == null) return;

        BigDecimal threshold;
        try { threshold = new BigDecimal(thresholdStr); } catch (NumberFormatException e) { return; }

        BigDecimal value = "weight".equals(field) ? event.getWeight() : event.getQuantity();
        if (value != null && value.compareTo(threshold) > 0) {
            failures.add(ValidationFailure.builder()
                .entityId(event.getId())
                .entityType("BILLING_EVENT")
                .ruleCode("QUANTITY_THRESHOLD_EXCEEDED")
                .rule("QUANTITY_THRESHOLD")
                .field(field)
                .severity(rule.isBlocking() ? ValidationSeverity.BLOCKING : ValidationSeverity.WARNING)
                .description(field + " value " + value + " exceeds configured threshold of "
                    + threshold + " on event " + event.getId())
                .build());
        }
    }

    private void checkClassification(BillingEvent event, ValidationRule rule,
                                      List<ValidationFailure> failures) {
        if (event.getLegalClassification() == null) {
            failures.add(ValidationFailure.builder()
                .entityId(event.getId())
                .entityType("BILLING_EVENT")
                .ruleCode("MISSING_CLASSIFICATION")
                .rule("CLASSIFICATION")
                .field("legalClassification")
                .severity(rule.isBlocking() ? ValidationSeverity.BLOCKING : ValidationSeverity.WARNING)
                .description("Legal classification is not set on event " + event.getId())
                .build());
        }
    }

    private static final Pattern CONFIG_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"");

    private String configValue(ValidationRule rule, String key) {
        if (rule.getConfig() == null) return null;
        Matcher m = CONFIG_PATTERN.matcher(rule.getConfig());
        while (m.find()) {
            if (key.equals(m.group(1))) return m.group(2);
        }
        // Also try numeric values (no quotes around value)
        Pattern numericPattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*([\\d.]+)");
        Matcher nm = numericPattern.matcher(rule.getConfig());
        if (nm.find()) return nm.group(1);
        return null;
    }
}
