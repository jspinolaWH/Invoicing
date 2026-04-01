package com.example.invoicing.repository;

import com.example.invoicing.entity.vat.VatRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VatRateRepository extends JpaRepository<VatRate, Long> {

    @Query("SELECT v FROM VatRate v WHERE v.validFrom <= :eventDate AND (v.validTo IS NULL OR v.validTo >= :eventDate)")
    List<VatRate> findByEventDate(@Param("eventDate") LocalDate eventDate);

    @Query("SELECT v FROM VatRate v WHERE v.validFrom <= :today AND (v.validTo IS NULL OR v.validTo >= :today)")
    List<VatRate> findActive(@Param("today") LocalDate today);
}
