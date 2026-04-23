package com.example.invoicing.repository;

import com.example.invoicing.entity.driver.DriverMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriverMasterRepository extends JpaRepository<DriverMaster, String> {

    List<DriverMaster> findTop10ByActiveIsTrueAndDriverIdContainingIgnoreCaseOrActiveIsTrueAndNameContainingIgnoreCase(
            String driverId, String name);
}
