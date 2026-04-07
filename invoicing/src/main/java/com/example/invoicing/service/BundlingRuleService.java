package com.example.invoicing.service;
import com.example.invoicing.entity.bundling.BundlingType;
import com.example.invoicing.repository.BundlingRuleRepository;
import com.example.invoicing.entity.bundling.BundlingRule;

import com.example.invoicing.entity.bundling.dto.BundlingRuleRequest;
import com.example.invoicing.entity.bundling.dto.BundlingRuleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BundlingRuleService {

    private final BundlingRuleRepository repository;

    @Transactional(readOnly = true)
    public List<BundlingRuleResponse> findByCustomer(String customerNumber) {
        return repository.findByCustomerNumber(customerNumber)
            .stream().map(this::toResponse).toList();
    }

    public List<BundlingRuleResponse> replaceAll(String customerNumber, List<BundlingRuleRequest> requests) {
        repository.deleteByCustomerNumber(customerNumber);
        List<BundlingRule> newRules = requests.stream()
            .map(r -> BundlingRule.builder()
                .customerNumber(customerNumber)
                .productGroup(r.getProductGroup())
                .bundlingType(r.getBundlingType())
                .description(r.getDescription())
                .build())
            .toList();
        return repository.saveAll(newRules).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BundlingType resolveForProductGroup(String customerNumber, String productGroup) {
        return repository.findByCustomerNumberAndProductGroup(customerNumber, productGroup)
            .map(BundlingRule::getBundlingType)
            .orElse(BundlingType.SEPARATE);
    }

    private BundlingRuleResponse toResponse(BundlingRule r) {
        return BundlingRuleResponse.builder()
            .id(r.getId())
            .customerNumber(r.getCustomerNumber())
            .productGroup(r.getProductGroup())
            .bundlingType(r.getBundlingType())
            .description(r.getDescription())
            .build();
    }
}
