package com.example.invoicing.repository;

import com.example.invoicing.entity.receivingsite.ReceivingSite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReceivingSiteRepository extends JpaRepository<ReceivingSite, Long> {

    @Query("""
        SELECT s FROM ReceivingSite s
        WHERE s.active = true
          AND (LOWER(s.name)             LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(s.municipalityName) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY s.name
        LIMIT 10
        """)
    List<ReceivingSite> searchSites(@Param("q") String q);
}
