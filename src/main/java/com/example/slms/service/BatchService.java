package com.example.slms.service;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import com.example.slms.dto.request.BatchJobRequest;
import com.example.slms.dto.response.BatchJobResponse;

public interface BatchService {

	BatchJobResponse scheduleJob(BatchJobRequest request);

	BatchJobResponse runJob(BatchJobRequest request);

	BatchJobResponse retryJob(BatchJobRequest request);

	BatchJobResponse uploadImportFile(MultipartFile file);

	Page<BatchJobResponse> listLogs(int page, int size, String jobType);
}
