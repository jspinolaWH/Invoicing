package com.example.invoicing.controller.account;

import com.example.invoicing.entity.account.AccountingAccount;
import com.example.invoicing.entity.account.dto.AccountingAccountRequest;
import com.example.invoicing.entity.account.dto.AccountingAccountResponse;
import com.example.invoicing.service.AccountingAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounting-accounts")
@RequiredArgsConstructor
public class AccountingAccountController {

    private final AccountingAccountService service;

    @GetMapping
    public List<AccountingAccountResponse> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate activeOn,
            @RequestParam(required = false) Boolean active) {

        List<AccountingAccount> accounts;
        if (activeOn != null) {
            accounts = service.findActiveOn(activeOn);
        } else if (Boolean.TRUE.equals(active)) {
            accounts = service.findCurrentlyActive();
        } else {
            accounts = service.findAll();
        }
        return accounts.stream().map(AccountingAccountResponse::from).toList();
    }

    @GetMapping("/search")
    public List<AccountingAccountResponse> search(@RequestParam String q) {
        return service.search(q).stream().map(AccountingAccountResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AccountingAccountResponse getById(@PathVariable Long id) {
        return AccountingAccountResponse.from(service.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountingAccountResponse create(@RequestBody AccountingAccountRequest request) {
        return AccountingAccountResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    public AccountingAccountResponse update(@PathVariable Long id, @RequestBody AccountingAccountRequest request) {
        return AccountingAccountResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
