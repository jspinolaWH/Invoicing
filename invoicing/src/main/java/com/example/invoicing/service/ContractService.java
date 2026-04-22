package com.example.invoicing.service;

import com.example.invoicing.entity.contract.Contract;
import com.example.invoicing.entity.contract.dto.ContractSummaryDto;
import com.example.invoicing.entity.product.dto.InvoicingProductResponse;
import com.example.invoicing.repository.ContractRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractService {

    private final ContractRepository contractRepository;

    public List<ContractSummaryDto> getContractsForCustomer(String customerNumber) {
        return contractRepository.findByCustomerNumberAndActiveTrue(customerNumber).stream()
            .map(this::toSummary)
            .toList();
    }

    @Transactional
    public ContractSummaryDto updateTemplate(Long contractId, Long invoiceTemplateId) {
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new EntityNotFoundException("Contract not found: " + contractId));
        contract.setInvoiceTemplateId(invoiceTemplateId);
        return toSummary(contractRepository.save(contract));
    }

    @Transactional
    public ContractSummaryDto updateWorkSite(Long contractId, String workSite) {
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new EntityNotFoundException("Contract not found: " + contractId));
        contract.setWorkSite(workSite);
        return toSummary(contractRepository.save(contract));
    }

    @Transactional
    public ContractSummaryDto updateDates(Long contractId, LocalDate startDate, LocalDate endDate) {
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new EntityNotFoundException("Contract not found: " + contractId));
        contract.setStartDate(startDate);
        contract.setEndDate(endDate);
        return toSummary(contractRepository.save(contract));
    }

    public List<InvoicingProductResponse> getProductsForContract(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new EntityNotFoundException("Contract not found: " + contractId));
        return contract.getProducts().stream()
            .map(InvoicingProductResponse::from)
            .toList();
    }

    private ContractSummaryDto toSummary(Contract c) {
        return ContractSummaryDto.builder()
            .id(c.getId())
            .name(c.getName())
            .customerNumber(c.getCustomerNumber())
            .active(c.isActive())
            .invoiceTemplateId(c.getInvoiceTemplateId())
            .workSite(c.getWorkSite())
            .startDate(c.getStartDate())
            .endDate(c.getEndDate())
            .build();
    }
}
