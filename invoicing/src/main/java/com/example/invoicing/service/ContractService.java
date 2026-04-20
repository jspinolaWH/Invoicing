package com.example.invoicing.service;

import com.example.invoicing.entity.contract.Contract;
import com.example.invoicing.entity.contract.dto.ContractSummaryDto;
import com.example.invoicing.entity.product.dto.InvoicingProductResponse;
import com.example.invoicing.repository.ContractRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractService {

    private final ContractRepository contractRepository;

    public List<ContractSummaryDto> getContractsForCustomer(String customerNumber) {
        return contractRepository.findByCustomerNumberAndActiveTrue(customerNumber).stream()
            .map(c -> ContractSummaryDto.builder()
                .id(c.getId())
                .name(c.getName())
                .customerNumber(c.getCustomerNumber())
                .active(c.isActive())
                .build())
            .toList();
    }

    public List<InvoicingProductResponse> getProductsForContract(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new EntityNotFoundException("Contract not found: " + contractId));
        return contract.getProducts().stream()
            .map(InvoicingProductResponse::from)
            .toList();
    }
}
