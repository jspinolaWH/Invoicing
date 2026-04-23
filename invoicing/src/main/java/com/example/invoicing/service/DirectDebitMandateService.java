package com.example.invoicing.service;

import com.example.invoicing.entity.customer.dto.DirectDebitMandateResponse;
import com.example.invoicing.repository.DirectDebitMandateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DirectDebitMandateService {

    private final DirectDebitMandateRepository mandateRepo;

    @Transactional(readOnly = true)
    public DirectDebitMandateResponse getMandate(Long customerId) {
        return mandateRepo.findByCustomer_Id(customerId)
            .map(DirectDebitMandateResponse::from)
            .orElse(DirectDebitMandateResponse.empty(customerId));
    }
}
