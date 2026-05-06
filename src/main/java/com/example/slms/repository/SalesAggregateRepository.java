package com.example.slms.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.slms.entity.SalesAggregate;

public interface SalesAggregateRepository extends JpaRepository<SalesAggregate, Long> {

    Optional<SalesAggregate> findByReportDate(LocalDate reportDate);
}
