package com.example.invoicing.service;

import com.example.invoicing.entity.customer.Customer;
import com.example.invoicing.repository.CustomerBillingProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerBillingProfileRepository customerRepo;

    public void validateParentExists(String parentCustomerNumber) {
        if (parentCustomerNumber == null || parentCustomerNumber.isBlank()) return;
        boolean exists = customerRepo
            .findByBillingProfile_CustomerIdNumber(parentCustomerNumber)
            .isPresent();
        if (!exists) {
            throw new EntityNotFoundException(
                "Parent customer with number " + parentCustomerNumber + " does not exist.");
        }
    }

    public List<String> resolveHierarchyChain(String customerNumber) {
        List<String> chain = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        String current = customerNumber;
        while (current != null && visited.add(current)) {
            chain.add(current);
            current = customerRepo
                .findByBillingProfile_CustomerIdNumber(current)
                .map(Customer::getParentCustomerNumber)
                .orElse(null);
        }
        return chain;
    }
}
