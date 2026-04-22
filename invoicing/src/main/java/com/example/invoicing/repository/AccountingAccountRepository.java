package com.example.invoicing.repository;

import com.example.invoicing.entity.account.AccountingAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccountingAccountRepository extends JpaRepository<AccountingAccount, Long> {

    @Query("SELECT a FROM AccountingAccount a WHERE a.validFrom <= :date AND (a.validTo IS NULL OR a.validTo >= :date)")
    List<AccountingAccount> findActiveOn(@Param("date") LocalDate date);

    @Query("SELECT a FROM AccountingAccount a WHERE a.validFrom <= :today AND (a.validTo IS NULL OR a.validTo >= :today)")
    List<AccountingAccount> findCurrentlyActive(@Param("today") LocalDate today);

    Optional<AccountingAccount> findByCode(String code);

    @Query("SELECT a FROM AccountingAccount a WHERE LOWER(a.code) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(a.name) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY a.code")
    List<AccountingAccount> searchByCodeOrName(@Param("q") String q);
}
