package com.cognivuex.repository;

import com.cognivuex.entity.HealthReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HealthReportRepository extends JpaRepository<HealthReport, Long> {

    Optional<HealthReport> findTopByOrderByIdDesc();

}