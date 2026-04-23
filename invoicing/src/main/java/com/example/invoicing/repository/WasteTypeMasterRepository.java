package com.example.invoicing.repository;

import com.example.invoicing.entity.wastetype.WasteTypeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WasteTypeMasterRepository extends JpaRepository<WasteTypeMaster, String> {

    @Query("""
        SELECT w FROM WasteTypeMaster w
        WHERE w.active = true
          AND (LOWER(w.code)   LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(w.nameFi) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(w.nameEn) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY w.code
        LIMIT 10
        """)
    List<WasteTypeMaster> searchWasteTypes(@Param("q") String q);
}
