package com.example.slms.service;

import org.springframework.data.domain.Page;

import com.example.slms.dto.request.BatchJobRequest;
import com.example.slms.dto.response.BatchJobResponse;

public interface BatchService {

	BatchJobResponse scheduleJob(BatchJobRequest request);

	BatchJobResponse runJob(BatchJobRequest request);

	BatchJobResponse retryJob(BatchJobRequest request);

	Page<BatchJobResponse> listLogs(int page, int size, String jobType);
}
