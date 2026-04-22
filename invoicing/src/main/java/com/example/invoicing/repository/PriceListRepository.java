package com.example.invoicing.repository;

import com.example.invoicing.entity.pricelist.PriceList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PriceListRepository extends JpaRepository<PriceList, Long> {

    Optional<PriceList> findByCode(String code);

    @Query("SELECT p FROM PriceList p WHERE p.active = true AND p.validFrom <= :date AND (p.validTo IS NULL OR p.validTo >= :date)")
    List<PriceList> findActiveOn(@Param("date") LocalDate date);

    List<PriceList> findByActiveTrue();
}
