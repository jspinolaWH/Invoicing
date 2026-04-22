package com.example.invoicing.repository;

import com.example.invoicing.entity.project.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByCustomerNumber(String customerNumber);
    List<Project> findByCustomerNumberAndActiveTrue(String customerNumber);
}
