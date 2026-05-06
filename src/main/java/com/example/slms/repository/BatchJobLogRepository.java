package com.example.slms.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.slms.entity.BatchJobLog;

public interface BatchJobLogRepository extends JpaRepository<BatchJobLog, Long> {

    Page<BatchJobLog> findByJobType(String jobType, Pageable pageable);

    Optional<BatchJobLog> findTopByJobTypeAndStatusOrderByCreatedAtDesc(String jobType, String status);
}
