package com.example.invoicing.accounting;

import com.example.invoicing.entity.allocation.AccountingAllocationRule;
import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.repository.AccountingAllocationRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountingAllocationService {

    private final AccountingAllocationRuleRepository ruleRepository;

    public List<LedgerEntry> resolveEntries(BillingEvent event) {
        List<ComponentAmount> components = splitByPriceComponent(event);
        List<LedgerEntry> entries = new ArrayList<>();

        for (ComponentAmount ca : components) {
            AccountingAllocationRule rule = findBestRule(
                event.getProduct().getId(),
                event.getMunicipalityId(),
                event.getMunicipalityId());

            BigDecimal vatRate = event.getVatRate24() != null ? event.getVatRate24() : BigDecimal.ZERO;
            BigDecimal amountVat = computeVatAmount(ca.amount(), vatRate);
            BigDecimal amountGross = ca.amount().add(amountVat);

            entries.add(LedgerEntry.builder()
                .billingEventId(event.getId())
                .accountingAccount(rule.getAccountingAccount())
                .amountNet(ca.amount())
                .amountVat(amountVat)
                .amountGross(amountGross)
                .vatRate(vatRate)
                .priceComponent(ca.component())
                .description(ca.component().name().replace("_", " ") + " — Event #" + event.getId())
                .matchedRuleId(rule.getId())
                .build());
        }
        return entries;
    }

    public AccountingAllocationRule findBestRule(Long productId, String region, String municipality) {
        List<AccountingAllocationRule> rules = ruleRepository.findMostSpecificRules(
            productId, region, municipality);
        if (rules.isEmpty()) {
            throw new AllocationRuleNotFoundException(productId, region);
        }
        return rules.get(0);
    }

    public BigDecimal computeVatAmount(BigDecimal netAmount, BigDecimal vatRatePercent) {
        return netAmount.multiply(vatRatePercent)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private List<ComponentAmount> splitByPriceComponent(BillingEvent event) {
        List<ComponentAmount> components = new ArrayList<>();
        if (event.getWasteFeePrice() != null && event.getWasteFeePrice().compareTo(BigDecimal.ZERO) > 0) {
            components.add(new ComponentAmount(PriceComponent.WASTE_FEE, event.getWasteFeePrice()));
        }
        if (event.getTransportFeePrice() != null && event.getTransportFeePrice().compareTo(BigDecimal.ZERO) > 0) {
            components.add(new ComponentAmount(PriceComponent.TRANSPORT_FEE, event.getTransportFeePrice()));
        }
        if (event.getEcoFeePrice() != null && event.getEcoFeePrice().compareTo(BigDecimal.ZERO) > 0) {
            components.add(new ComponentAmount(PriceComponent.ECO_FEE, event.getEcoFeePrice()));
        }
        return components;
    }

    private record ComponentAmount(PriceComponent component, BigDecimal amount) {}
}
