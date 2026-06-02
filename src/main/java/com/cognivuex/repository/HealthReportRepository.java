package com.cognivuex.repository;

import com.cognivuex.entity.HealthReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthReportRepository
        extends JpaRepository<HealthReport, Long> {
}