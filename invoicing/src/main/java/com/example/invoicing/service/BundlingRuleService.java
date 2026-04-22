package com.example.invoicing.service;

import com.example.invoicing.entity.bundling.BundlingRule;
import com.example.invoicing.entity.bundling.BundlingRuleAuditLog;
import com.example.invoicing.entity.bundling.BundlingType;
import com.example.invoicing.entity.bundling.dto.BundlingRuleRequest;
import com.example.invoicing.entity.bundling.dto.BundlingRuleResponse;
import com.example.invoicing.repository.BundlingRuleAuditLogRepository;
import com.example.invoicing.repository.BundlingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BundlingRuleService {

    private final BundlingRuleRepository repository;
    private final BundlingRuleAuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public List<BundlingRuleResponse> findByCustomer(String customerNumber) {
        return repository.findByCustomerNumber(customerNumber)
            .stream().map(this::toResponse).toList();
    }

    public List<BundlingRuleResponse> replaceAll(String customerNumber, List<BundlingRuleRequest> requests, String changedBy) {
        Map<String, BundlingRule> existing = repository.findByCustomerNumber(customerNumber)
            .stream().collect(Collectors.toMap(BundlingRule::getProductGroup, r -> r));

        Map<String, BundlingRuleRequest> incoming = requests.stream()
            .collect(Collectors.toMap(BundlingRuleRequest::getProductGroup, r -> r));

        List<BundlingRuleAuditLog> auditEntries = new ArrayList<>();
        Instant now = Instant.now();

        // update or add
        for (BundlingRuleRequest req : requests) {
            BundlingRule current = existing.get(req.getProductGroup());
            if (current == null) {
                BundlingRule saved = repository.save(BundlingRule.builder()
                    .customerNumber(customerNumber)
                    .productGroup(req.getProductGroup())
                    .bundlingType(req.getBundlingType())
                    .description(req.getDescription())
                    .build());
                auditEntries.add(BundlingRuleAuditLog.builder()
                    .customerNumber(customerNumber)
                    .productGroup(req.getProductGroup())
                    .action("ADDED")
                    .oldValue(null)
                    .newValue(req.getBundlingType().name())
                    .changedBy(changedBy)
                    .changedAt(now)
                    .build());
            } else if (current.getBundlingType() != req.getBundlingType()
                    || !eq(current.getDescription(), req.getDescription())) {
                auditEntries.add(BundlingRuleAuditLog.builder()
                    .customerNumber(customerNumber)
                    .productGroup(req.getProductGroup())
                    .action("CHANGED")
                    .oldValue(current.getBundlingType().name())
                    .newValue(req.getBundlingType().name())
                    .changedBy(changedBy)
                    .changedAt(now)
                    .build());
                current.setBundlingType(req.getBundlingType());
                current.setDescription(req.getDescription());
                repository.save(current);
            }
        }

        // remove rules not in incoming set
        Set<String> incomingGroups = incoming.keySet();
        for (BundlingRule rule : existing.values()) {
            if (!incomingGroups.contains(rule.getProductGroup())) {
                auditEntries.add(BundlingRuleAuditLog.builder()
                    .customerNumber(customerNumber)
                    .productGroup(rule.getProductGroup())
                    .action("REMOVED")
                    .oldValue(rule.getBundlingType().name())
                    .newValue(null)
                    .changedBy(changedBy)
                    .changedAt(now)
                    .build());
                repository.delete(rule);
            }
        }

        if (!auditEntries.isEmpty()) {
            auditLogRepository.saveAll(auditEntries);
        }

        return repository.findByCustomerNumber(customerNumber).stream().map(this::toResponse).toList();
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

    private static boolean eq(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
