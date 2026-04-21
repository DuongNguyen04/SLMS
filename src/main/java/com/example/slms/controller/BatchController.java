package com.example.slms.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.slms.dto.request.BatchJobRequest;
import com.example.slms.dto.response.BatchJobResponse;
import com.example.slms.service.BatchService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/batch")
@Validated
@RequiredArgsConstructor
public class BatchController {

	private final BatchService batchService;

	@PostMapping("/jobs/schedule")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<BatchJobResponse> scheduleJob(@Valid @RequestBody BatchJobRequest request) {
		BatchJobResponse response = batchService.scheduleJob(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/jobs/run")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<BatchJobResponse> runJob(@Valid @RequestBody BatchJobRequest request) {
		BatchJobResponse response = batchService.runJob(request);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/jobs/retry")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<BatchJobResponse> retryJob(@Valid @RequestBody BatchJobRequest request) {
		BatchJobResponse response = batchService.retryJob(request);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/logs")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Page<BatchJobResponse>> listLogs(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) int size,
			@RequestParam(required = false) String jobType) {
		Page<BatchJobResponse> response = batchService.listLogs(page, size, jobType);
		return ResponseEntity.ok(response);
	}
}
