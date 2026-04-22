package com.example.invoicing.service;

import com.example.invoicing.entity.reporting.ReportingFieldConfig;
import com.example.invoicing.entity.reporting.dto.ReportingFieldConfigDto;
import com.example.invoicing.repository.ReportingFieldConfigRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportingFieldConfigService {

    private final ReportingFieldConfigRepository repository;

    @Transactional(readOnly = true)
    public List<ReportingFieldConfigDto> findAll(Long companyId) {
        return repository.findByCompanyIdOrderByDisplayOrderAsc(companyId)
            .stream().map(this::toDto).toList();
    }

    public ReportingFieldConfigDto upsert(Long companyId, ReportingFieldConfigDto req) {
        ReportingFieldConfig config = repository.findByCompanyIdAndFieldName(companyId, req.getFieldName())
            .orElseGet(ReportingFieldConfig::new);
        config.setCompanyId(companyId);
        config.setFieldName(req.getFieldName());
        config.setLabelOverride(req.getLabelOverride());
        config.setEnabled(req.isEnabled());
        config.setDisplayOrder(req.getDisplayOrder());
        return toDto(repository.save(config));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("ReportingFieldConfig not found: " + id);
        }
        repository.deleteById(id);
    }

    private ReportingFieldConfigDto toDto(ReportingFieldConfig c) {
        ReportingFieldConfigDto dto = new ReportingFieldConfigDto();
        dto.setId(c.getId());
        dto.setCompanyId(c.getCompanyId());
        dto.setFieldName(c.getFieldName());
        dto.setLabelOverride(c.getLabelOverride());
        dto.setEnabled(c.isEnabled());
        dto.setDisplayOrder(c.getDisplayOrder());
        return dto;
    }
}
