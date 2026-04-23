package com.example.invoicing.repository;

import com.example.invoicing.entity.vehicle.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, String> {

    List<Vehicle> findTop10ByActiveIsTrueAndVehicleIdContainingIgnoreCaseOrActiveIsTrueAndRegistrationPlateContainingIgnoreCase(
            String vehicleId, String registrationPlate);
}
