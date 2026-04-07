package com.example.invoicing.service;
import com.example.invoicing.entity.classification.*;
import com.example.invoicing.entity.customer.Customer;
import com.example.invoicing.entity.customer.CustomerType;
import com.example.invoicing.entity.product.Product;
import com.example.invoicing.repository.ClassificationRuleRepository;
import com.example.invoicing.repository.CustomerBillingProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LegalClassificationService {
    private final ClassificationRuleRepository ruleRepository;
    private final CustomerBillingProfileRepository customerRepository;

    public LegalClassification evaluate(Long companyId, CustomerType customerType,
                                         String productCode, String regionCode,
                                         LegalClassification companyDefault) {
        List<ClassificationRule> rules =
            ruleRepository.findByCompanyIdAndActiveTrueOrderByPriorityAscIdAsc(companyId);
        for (ClassificationRule rule : rules) {
            if (matches(rule, customerType, productCode, regionCode)) return rule.getResultClassification();
        }
        return companyDefault;
    }

    /**
     * Convenience method for BillingEventService — resolves classification from a customer number.
     * Uses company 1L as the default company; falls back to PRIVATE_LAW if customer not found.
     */
    public LegalClassification classify(String customerNumber, Product product, String regionCode) {
        CustomerType customerType = customerRepository
            .findByBillingProfile_CustomerIdNumber(customerNumber)
            .map(Customer::getCustomerType)
            .orElse(CustomerType.PRIVATE);
        String productCode = product != null ? product.getCode() : null;
        return evaluate(1L, customerType, productCode, regionCode, LegalClassification.PRIVATE_LAW);
    }

    /**
     * Classify a BillingEvent — convenience wrapper for InvoiceGenerationService.
     */
    public LegalClassification classify(com.example.invoicing.entity.billingevent.BillingEvent event) {
        if (event == null) return LegalClassification.PRIVATE_LAW;
        return classify(event.getCustomerNumber(), event.getProduct(), event.getMunicipalityId());
    }

    private boolean matches(ClassificationRule rule, CustomerType customerType,
                             String productCode, String regionCode) {
        if (rule.getCustomerTypeCondition() != null && rule.getCustomerTypeCondition() != customerType) return false;
        if (rule.getProductCodeCondition() != null && !rule.getProductCodeCondition().equals(productCode)) return false;
        if (rule.getRegionCondition() != null && !rule.getRegionCondition().equals(regionCode)) return false;
        return true;
    }
}
