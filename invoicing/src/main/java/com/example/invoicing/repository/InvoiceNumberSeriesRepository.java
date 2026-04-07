package com.example.invoicing.repository;

import com.example.invoicing.entity.numberseries.InvoiceNumberSeries;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvoiceNumberSeriesRepository extends JpaRepository<InvoiceNumberSeries, Long> {

    // CRITICAL: pessimistic write lock prevents concurrent threads from getting the same counter value
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InvoiceNumberSeries s WHERE s.id = :id")
    Optional<InvoiceNumberSeries> findByIdForUpdate(@Param("id") Long id);

    Optional<InvoiceNumberSeries> findByName(String name);
}
