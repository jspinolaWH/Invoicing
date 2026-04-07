package com.example.invoicing.repository;
import com.example.invoicing.entity.invoice.Invoice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("SELECT i FROM Invoice i WHERE i.customer.id = :customerId AND i.status IN ('DRAFT', 'READY', 'SENT')")
    List<Invoice> findOpenByCustomerId(@Param("customerId") Long customerId);

    List<Invoice> findByInvoiceRunId(Long invoiceRunId);

    @Query("SELECT i FROM Invoice i WHERE i.originalInvoice.id = :invoiceId AND i.invoiceType = 'CREDIT_NOTE'")
    List<Invoice> findCreditNotesByOriginalInvoiceId(@Param("invoiceId") Long invoiceId);

    @Query("SELECT i FROM Invoice i WHERE i.customer.id = :customerId AND i.status IN ('SENT', 'COMPLETED') ORDER BY i.invoiceDate DESC")
    Page<Invoice> findSentOrCompletedByCustomerId(@Param("customerId") Long customerId, Pageable pageable);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    @Query("SELECT i FROM Invoice i WHERE i.customer.id = :customerId AND i.invoiceType = 'CREDIT_NOTE' ORDER BY i.invoiceDate DESC")
    Page<Invoice> findCreditNotesByCustomerId(@Param("customerId") Long customerId, Pageable pageable);

    @Query("""
        SELECT i FROM Invoice i
        WHERE i.status IN ('SENT', 'COMPLETED')
          AND (:customerId IS NULL OR i.customer.id = :customerId)
          AND (:dateFrom IS NULL OR i.invoiceDate >= :dateFrom)
          AND (:dateTo IS NULL OR i.invoiceDate <= :dateTo)
        ORDER BY i.invoiceDate DESC
        """)
    Page<Invoice> findForAuthority(
        @Param("customerId") Long customerId,
        @Param("dateFrom") java.time.LocalDate dateFrom,
        @Param("dateTo") java.time.LocalDate dateTo,
        org.springframework.data.domain.Pageable pageable
    );
}
