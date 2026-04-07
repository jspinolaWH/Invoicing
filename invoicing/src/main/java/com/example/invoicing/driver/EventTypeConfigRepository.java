package com.example.invoicing.driver;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventTypeConfigRepository extends JpaRepository<EventTypeConfig, Long> {

    Optional<EventTypeConfig> findByEventTypeCode(String eventTypeCode);
}
