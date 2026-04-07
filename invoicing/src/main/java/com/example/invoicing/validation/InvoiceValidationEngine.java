package com.example.invoicing.validation;

import com.example.invoicing.entity.validation.*;
import com.example.invoicing.invoice.Invoice;
import com.example.invoicing.invoice.InvoiceLineItem;
import com.example.invoicing.invoice.InvoiceType;
import com.example.invoicing.repository.ValidationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceValidationEngine {

    private final ValidationRuleRepository ruleRepository;

    public ValidationReport validate(Invoice invoice, Long companyId) {
        List<ValidationRule> rules = ruleRepository.findAllActiveByCompanyId(companyId);
        List<ValidationFailure> failures = new ArrayList<>();

        for (ValidationRule rule : rules) {
            for (InvoiceLineItem line : invoice.getLineItems()) {
                Optional<String> failure = evaluate(rule, line, invoice);
                failure.ifPresent(msg -> failures.add(ValidationFailure.builder()
                    .entityId(line.getId())
                    .entityType("INVOICE_LINE")
                    .field(rule.getRuleCode())
                    .rule(rule.getRuleCode())
                    .ruleCode(rule.getRuleCode())
                    .severity(rule.isBlocking() ? ValidationSeverity.BLOCKING : ValidationSeverity.WARNING)
                    .description(msg)
                    .build()));
            }
        }

        int totalChecked = invoice.getLineItems().size();
        long blockingCount = failures.stream().filter(f -> f.getSeverity() == ValidationSeverity.BLOCKING).count();
        long warningCount = failures.stream().filter(f -> f.getSeverity() == ValidationSeverity.WARNING).count();

        return ValidationReport.builder()
            .totalChecked(totalChecked)
            .passed(totalChecked - failures.size())
            .blockingFailureCount((int) blockingCount)
            .warningCount((int) warningCount)
            .failures(failures)
            .build();
    }

    private Optional<String> evaluate(ValidationRule rule, InvoiceLineItem line, Invoice invoice) {
        return switch (rule.getRuleType()) {
            case MANDATORY_FIELD -> evaluateMandatoryField(rule, line);
            case PRICE_CONSISTENCY -> evaluatePriceConsistency(line, invoice);
            case QUANTITY_THRESHOLD -> evaluateQuantityThreshold(rule, line);
            case CLASSIFICATION -> evaluateClassification(line);
        };
    }

    private Optional<String> evaluateMandatoryField(ValidationRule rule, InvoiceLineItem line) {
        String field = extractJsonString(rule.getConfig(), "field");
        if (field == null) return Optional.empty();
        Object value = switch (field) {
            case "costCenter" -> line.getCostCenter();
            case "legalClassification" -> line.getLegalClassification();
            case "accountingAccount" -> line.getAccountingAccount();
            default -> null;
        };
        if (value == null || (value instanceof String s && s.isBlank())) {
            return Optional.of("Field '" + field + "' is required but missing on line item " + line.getId());
        }
        return Optional.empty();
    }

    private Optional<String> evaluatePriceConsistency(InvoiceLineItem line, Invoice invoice) {
        boolean isCreditNote = invoice.getInvoiceType() == InvoiceType.CREDIT_NOTE;
        if (!isCreditNote && line.getUnitPrice() != null &&
            line.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            return Optional.of("Unit price must be positive on non-credit invoice, line " + line.getId());
        }
        return Optional.empty();
    }

    private Optional<String> evaluateQuantityThreshold(ValidationRule rule, InvoiceLineItem line) {
        String thresholdStr = extractJsonString(rule.getConfig(), "threshold");
        if (thresholdStr == null) return Optional.empty();
        try {
            BigDecimal threshold = new BigDecimal(thresholdStr);
            if (line.getQuantity() != null && line.getQuantity().compareTo(threshold) > 0) {
                return Optional.of("Quantity " + line.getQuantity() + " exceeds threshold "
                    + threshold + " on line " + line.getId());
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse threshold in rule config: {}", rule.getConfig());
        }
        return Optional.empty();
    }

    /** Simple extraction of a JSON string or number value by key. */
    private String extractJsonString(String json, String key) {
        if (json == null || json.isBlank()) return null;
        // Match "key": "value" or "key": value
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"?([^\"\\},]+)\"?");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private Optional<String> evaluateClassification(InvoiceLineItem line) {
        if (line.getLegalClassification() == null) {
            return Optional.of("Legal classification is not set on line item " + line.getId());
        }
        return Optional.empty();
    }
}
