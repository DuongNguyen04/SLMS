package com.example.slms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.slms.entity.BatchJobSchedule;

public interface BatchJobScheduleRepository extends JpaRepository<BatchJobSchedule, Long> {

    Optional<BatchJobSchedule> findByJobType(String jobType);

    List<BatchJobSchedule> findByActiveTrue();
}
