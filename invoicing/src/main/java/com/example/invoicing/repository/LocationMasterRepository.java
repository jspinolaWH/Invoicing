package com.example.invoicing.repository;

import com.example.invoicing.entity.location.LocationMaster;
import com.example.invoicing.entity.location.dto.MunicipalityResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LocationMasterRepository extends JpaRepository<LocationMaster, String> {

    @Query("""
        SELECT l FROM LocationMaster l
        WHERE l.active = true
          AND (LOWER(l.locationId)       LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(l.name)             LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(l.municipalityName) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY l.locationId
        LIMIT 10
        """)
    List<LocationMaster> searchLocations(@Param("q") String q);

    @Query("""
        SELECT DISTINCT new com.example.invoicing.entity.location.dto.MunicipalityResult(
            l.municipalityId, l.municipalityName)
        FROM LocationMaster l
        WHERE l.active = true
          AND (LOWER(l.municipalityId)   LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(l.municipalityName) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY l.municipalityName
        LIMIT 10
        """)
    List<MunicipalityResult> searchMunicipalities(@Param("q") String q);
}
