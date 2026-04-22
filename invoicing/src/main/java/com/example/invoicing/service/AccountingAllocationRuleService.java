package com.example.invoicing.service;

import com.example.invoicing.entity.account.AccountingAccount;
import com.example.invoicing.entity.account.PriceComponent;
import com.example.invoicing.entity.allocation.AccountingAllocationRule;
import com.example.invoicing.entity.allocation.dto.AllocationResolveResponse;
import com.example.invoicing.entity.allocation.dto.AllocationRuleRequest;
import com.example.invoicing.entity.allocation.dto.AllocationRuleResponse;
import com.example.invoicing.entity.product.Product;
import com.example.invoicing.repository.AccountingAccountRepository;
import com.example.invoicing.repository.AccountingAllocationRuleRepository;
import com.example.invoicing.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountingAllocationRuleService {

    private final AccountingAllocationRuleRepository ruleRepository;
    private final ProductRepository productRepository;
    private final AccountingAccountRepository accountRepository;

    @Transactional(readOnly = true)
    public List<AllocationRuleResponse> findAll() {
        return ruleRepository.findAllByOrderBySpecificityScoreDesc()
            .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AllocationRuleResponse findById(Long id) {
        return toResponse(loadRule(id));
    }

    public AllocationRuleResponse create(AllocationRuleRequest request) {
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new EntityNotFoundException("Product not found: " + request.getProductId()));
        AccountingAccount account = accountRepository.findById(request.getAccountingAccountId())
            .orElseThrow(() -> new EntityNotFoundException("AccountingAccount not found: " + request.getAccountingAccountId()));

        AccountingAllocationRule rule = AccountingAllocationRule.builder()
            .product(product)
            .region(request.getRegion())
            .municipality(request.getMunicipality())
            .priceComponent(request.getPriceComponent())
            .accountingAccount(account)
            .specificityScore(computeSpecificityScore(request.getRegion(), request.getMunicipality(), request.getPriceComponent()))
            .description(request.getDescription())
            .active(true)
            .build();

        return toResponse(ruleRepository.save(rule));
    }

    public AllocationRuleResponse update(Long id, AllocationRuleRequest request) {
        AccountingAllocationRule rule = loadRule(id);

        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new EntityNotFoundException("Product not found: " + request.getProductId()));
        AccountingAccount account = accountRepository.findById(request.getAccountingAccountId())
            .orElseThrow(() -> new EntityNotFoundException("AccountingAccount not found: " + request.getAccountingAccountId()));

        rule.setProduct(product);
        rule.setRegion(request.getRegion());
        rule.setMunicipality(request.getMunicipality());
        rule.setPriceComponent(request.getPriceComponent());
        rule.setAccountingAccount(account);
        rule.setSpecificityScore(computeSpecificityScore(request.getRegion(), request.getMunicipality(), request.getPriceComponent()));
        rule.setDescription(request.getDescription());

        return toResponse(ruleRepository.save(rule));
    }

    public void delete(Long id) {
        AccountingAllocationRule rule = loadRule(id);
        rule.setActive(false);
        ruleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public Optional<AccountingAllocationRule> findBestMatch(Long productId, String region, String municipality, PriceComponent priceComponent) {
        List<AccountingAllocationRule> rules = ruleRepository.findMostSpecificRules(productId, region, municipality, priceComponent);
        return rules.isEmpty() ? Optional.empty() : Optional.of(rules.get(0));
    }

    @Transactional(readOnly = true)
    public AllocationResolveResponse resolve(Long productId, String region, String municipality, PriceComponent priceComponent) {
        Optional<AccountingAllocationRule> match = findBestMatch(productId, region, municipality, priceComponent);

        if (match.isEmpty()) {
            return AllocationResolveResponse.builder()
                .matchedRuleId(null)
                .matchReason("No matching rule found for this product/region combination.")
                .build();
        }

        AccountingAllocationRule rule = match.get();
        String componentSuffix = rule.getPriceComponent() != null ? " (component: " + rule.getPriceComponent().name() + ")" : "";
        String reason = switch (rule.getSpecificityScore()) {
            case 6, 3 -> "Matched on product + region + municipality" + componentSuffix;
            case 5, 2 -> "Matched on product + region" + componentSuffix;
            default   -> "Matched on product only (fallback rule)" + componentSuffix;
        };

        return AllocationResolveResponse.builder()
            .matchedRuleId(rule.getId())
            .specificityScore(rule.getSpecificityScore())
            .accountingAccountCode(rule.getAccountingAccount().getCode())
            .accountingAccountName(rule.getAccountingAccount().getName())
            .matchReason(reason)
            .build();
    }

    // -----------------------------------------------------------------------
    private int computeSpecificityScore(String region, String municipality, PriceComponent priceComponent) {
        int base;
        if (region != null && !region.isBlank() && municipality != null && !municipality.isBlank()) base = 3;
        else if (region != null && !region.isBlank()) base = 2;
        else base = 1;
        return priceComponent != null ? base + 3 : base;
    }

    private AccountingAllocationRule loadRule(Long id) {
        return ruleRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("AllocationRule not found: " + id));
    }

    private AllocationRuleResponse toResponse(AccountingAllocationRule r) {
        String productName = r.getProduct().getTranslations().stream()
            .filter(t -> "en".equals(t.getLocale())).findFirst()
            .map(t -> t.getName()).orElse(r.getProduct().getCode());
        return AllocationRuleResponse.builder()
            .id(r.getId())
            .productId(r.getProduct().getId())
            .productCode(r.getProduct().getCode())
            .productName(productName)
            .region(r.getRegion())
            .municipality(r.getMunicipality())
            .accountingAccountId(r.getAccountingAccount().getId())
            .accountingAccountCode(r.getAccountingAccount().getCode())
            .accountingAccountName(r.getAccountingAccount().getName())
            .priceComponent(r.getPriceComponent() != null ? r.getPriceComponent().name() : null)
            .specificityScore(r.getSpecificityScore())
            .description(r.getDescription())
            .active(r.isActive())
            .createdAt(r.getCreatedAt())
            .createdBy(r.getCreatedBy())
            .build();
    }
}
