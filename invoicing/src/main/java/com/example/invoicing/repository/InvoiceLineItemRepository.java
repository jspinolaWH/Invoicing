package com.example.invoicing.repository;

import com.example.invoicing.entity.invoice.InvoiceLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, Long> {

    @Query("SELECT li.accountingAccount.code, li.accountingAccount.name, SUM(li.netAmount), COUNT(li) " +
           "FROM InvoiceLineItem li " +
           "JOIN li.invoice i " +
           "WHERE li.accountingAccount IS NOT NULL " +
           "AND (:from IS NULL OR i.invoiceDate >= :from) " +
           "AND (:to IS NULL OR i.invoiceDate <= :to) " +
           "GROUP BY li.accountingAccount.code, li.accountingAccount.name " +
           "ORDER BY li.accountingAccount.code")
    List<Object[]> sumByAccount(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT li.costCenter.compositeCode, li.costCenter.description, SUM(li.netAmount), COUNT(li) " +
           "FROM InvoiceLineItem li " +
           "JOIN li.invoice i " +
           "WHERE li.costCenter IS NOT NULL " +
           "AND (:from IS NULL OR i.invoiceDate >= :from) " +
           "AND (:to IS NULL OR i.invoiceDate <= :to) " +
           "GROUP BY li.costCenter.compositeCode, li.costCenter.description " +
           "ORDER BY li.costCenter.compositeCode")
    List<Object[]> sumByCostCenter(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
