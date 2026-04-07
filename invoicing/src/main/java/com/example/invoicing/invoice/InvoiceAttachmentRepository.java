package com.example.invoicing.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InvoiceAttachmentRepository extends JpaRepository<InvoiceAttachment, Long> {

    List<InvoiceAttachment> findByInvoiceId(Long invoiceId);

    long countByInvoiceId(Long invoiceId);

    @Query("SELECT COALESCE(SUM(a.sizeBytes), 0) FROM InvoiceAttachment a WHERE a.invoice.id = :invoiceId")
    long sumSizeBytesByInvoiceId(@Param("invoiceId") Long invoiceId);
}
