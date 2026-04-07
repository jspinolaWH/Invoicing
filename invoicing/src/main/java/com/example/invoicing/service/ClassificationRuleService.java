package com.example.invoicing.service;
import com.example.invoicing.common.exception.DuplicatePriorityException;
import com.example.invoicing.entity.classification.ClassificationRule;
import com.example.invoicing.entity.classification.dto.*;
import com.example.invoicing.repository.ClassificationRuleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassificationRuleService {
    private final ClassificationRuleRepository ruleRepo;

    @Transactional(readOnly = true)
    public List<ClassificationRuleResponse> findAll(Long companyId) {
        return ruleRepo.findByCompanyIdOrderByPriorityAscIdAsc(companyId).stream()
            .map(ClassificationRuleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ClassificationRuleResponse findById(Long id) {
        return ClassificationRuleResponse.from(findRule(id));
    }

    public ClassificationRuleResponse create(Long companyId, ClassificationRuleRequest request) {
        if (ruleRepo.existsByCompanyIdAndPriorityAndIdNot(companyId, request.getPriority(), -1L))
            throw new DuplicatePriorityException("A rule with priority " + request.getPriority() + " already exists");
        return ClassificationRuleResponse.from(ruleRepo.save(request.toEntity(companyId)));
    }

    public ClassificationRuleResponse update(Long id, ClassificationRuleRequest request) {
        ClassificationRule rule = findRule(id);
        if (ruleRepo.existsByCompanyIdAndPriorityAndIdNot(rule.getCompanyId(), request.getPriority(), id))
            throw new DuplicatePriorityException("A rule with priority " + request.getPriority() + " already exists");
        request.applyTo(rule);
        return ClassificationRuleResponse.from(ruleRepo.save(rule));
    }

    public void delete(Long id) { ruleRepo.delete(findRule(id)); }

    public List<ClassificationRuleResponse> reorder(Long companyId, List<Long> orderedIds) {
        List<ClassificationRule> rules = ruleRepo.findAllById(orderedIds);
        for (int i = 0; i < orderedIds.size(); i++) {
            final int priority = i + 1;
            final Long ruleId = orderedIds.get(i);
            rules.stream().filter(r -> r.getId().equals(ruleId)).findFirst()
                 .ifPresent(r -> r.setPriority(priority));
        }
        ruleRepo.saveAll(rules);
        return findAll(companyId);
    }

    private ClassificationRule findRule(Long id) {
        return ruleRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Rule not found: " + id));
    }
}
