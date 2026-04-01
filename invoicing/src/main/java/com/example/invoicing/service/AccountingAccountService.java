package com.example.invoicing.service;

import com.example.invoicing.entity.account.AccountingAccount;
import com.example.invoicing.entity.account.dto.AccountingAccountRequest;
import com.example.invoicing.repository.AccountingAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountingAccountService {

    private final AccountingAccountRepository repository;

    public List<AccountingAccount> findAll() {
        return repository.findAll();
    }

    public List<AccountingAccount> findActiveOn(LocalDate date) {
        return repository.findActiveOn(date);
    }

    public List<AccountingAccount> findCurrentlyActive() {
        return repository.findCurrentlyActive(LocalDate.now());
    }

    public AccountingAccount findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AccountingAccount not found: " + id));
    }

    @Transactional
    public AccountingAccount create(AccountingAccountRequest request) {
        AccountingAccount account = new AccountingAccount();
        account.setCode(request.getCode());
        account.setName(request.getName());
        account.setValidFrom(request.getValidFrom());
        account.setValidTo(request.getValidTo());
        return repository.save(account);
    }

    @Transactional
    public AccountingAccount update(Long id, AccountingAccountRequest request) {
        AccountingAccount account = findById(id);
        account.setCode(request.getCode());
        account.setName(request.getName());
        account.setValidFrom(request.getValidFrom());
        account.setValidTo(request.getValidTo());
        return repository.save(account);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findById(id));
    }
}
