package com.example.bai3.repository;

import com.example.bai3.entity.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository
        extends JpaRepository<IncidentReport, Long> {
}