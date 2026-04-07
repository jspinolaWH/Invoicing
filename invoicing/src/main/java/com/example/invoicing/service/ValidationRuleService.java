package com.example.invoicing.service;
import com.example.invoicing.entity.validation.ValidationRule;
import com.example.invoicing.entity.validation.dto.*;
import com.example.invoicing.repository.ValidationRuleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ValidationRuleService {
    private final ValidationRuleRepository repository;

    public List<ValidationRuleResponse> findAll(Long companyId) {
        return repository.findByCompanyIdOrderByBlockingDescRuleCodeAsc(companyId)
            .stream().map(ValidationRuleResponse::from).toList();
    }

    @Transactional
    public ValidationRuleResponse create(ValidationRuleRequest request) {
        ValidationRule rule = new ValidationRule();
        applyRequest(rule, request);
        return ValidationRuleResponse.from(repository.save(rule));
    }

    @Transactional
    public ValidationRuleResponse update(Long id, ValidationRuleRequest request) {
        ValidationRule rule = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("ValidationRule not found: " + id));
        applyRequest(rule, request);
        return ValidationRuleResponse.from(repository.save(rule));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("ValidationRule not found: " + id)));
    }

    private void applyRequest(ValidationRule rule, ValidationRuleRequest req) {
        rule.setCompanyId(req.getCompanyId()); rule.setRuleType(req.getRuleType());
        rule.setRuleCode(req.getRuleCode()); rule.setConfig(req.getConfig());
        rule.setBlocking(req.isBlocking()); rule.setActive(req.isActive());
        rule.setDescription(req.getDescription());
    }
}
