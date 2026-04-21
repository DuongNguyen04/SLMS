package com.example.slms.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.slms.dto.request.BatchJobRequest;
import com.example.slms.dto.response.BatchJobResponse;
import com.example.slms.exception.BusinessException;
import com.example.slms.exception.ValidationException;
import com.example.slms.mapper.BatchMapper;
import com.example.slms.repository.CustomerOrderRepository;
import com.example.slms.repository.ProductRepository;
import com.example.slms.service.BatchService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BatchServiceImpl implements BatchService {

	private static final Set<String> SUPPORTED_JOB_TYPES = Set.of(
			"IMPORT_PRODUCT_DATA",
			"GENERATE_DAILY_REPORTS",
			"AGGREGATE_SALES_DATA");

	private final BatchMapper batchMapper;
	private final ProductRepository productRepository;
	private final CustomerOrderRepository customerOrderRepository;

	private final Map<String, String> scheduledJobs = new ConcurrentHashMap<>();
	private final List<BatchLogEntry> logs = new CopyOnWriteArrayList<>();

	@Override
	@Transactional
	public BatchJobResponse scheduleJob(BatchJobRequest request) {
		String jobType = normalizeJobType(request.getJobType());
		String cron = normalizeCron(request.getCron());

		scheduledJobs.put(jobType, cron);
		BatchJobResponse response = batchMapper.toResponse(jobType, "SCHEDULED", "Job scheduled: " + cron);
		addLog(response);
		return response;
	}

	@Override
	@Transactional
	public BatchJobResponse runJob(BatchJobRequest request) {
		String jobType = normalizeJobType(request.getJobType());
		BatchJobResponse response = executeJob(jobType);
		addLog(response);
		return response;
	}

	@Override
	@Transactional
	public BatchJobResponse retryJob(BatchJobRequest request) {
		String jobType = normalizeJobType(request.getJobType());
		int retryCount = request.getRetryCount() == null ? 1 : request.getRetryCount();
		if (retryCount <= 0) {
			throw new ValidationException("retryCount must be greater than 0");
		}

		BatchLogEntry latestFailed = findLatestFailedLog(jobType);
		if (latestFailed == null) {
			throw new ValidationException("Retry is not allowed because no failed job execution was found");
		}

		BatchJobResponse response = batchMapper.toResponse(
				jobType,
				"RETRY_ACCEPTED",
				"Retry accepted for latest failed execution");
		addLog(response);
		return response;
	}

	@Override
	@Transactional(readOnly = true)
	public Page<BatchJobResponse> listLogs(int page, int size, String jobType) {
		List<BatchLogEntry> entries = new ArrayList<>(logs);
		entries.sort((a, b) -> b.createdAt.compareTo(a.createdAt));

		final String normalizedJobType = (jobType != null && !jobType.trim().isEmpty())
				? normalizeJobType(jobType)
				: null;

		List<BatchJobResponse> filtered = entries.stream()
				.map(BatchLogEntry::response)
				.filter(item -> normalizedJobType == null || item.getJobType().equals(normalizedJobType))
				.toList();

		if (filtered.isEmpty()) {
			throw new BusinessException("No logs found", HttpStatus.NOT_FOUND);
		}

		int fromIndex = page * size;
		if (fromIndex >= filtered.size()) {
			throw new BusinessException("No logs found", HttpStatus.NOT_FOUND);
		}

		int toIndex = Math.min(fromIndex + size, filtered.size());
		List<BatchJobResponse> content = filtered.subList(fromIndex, toIndex);
		return new PageImpl<>(content, PageRequest.of(page, size), filtered.size());
	}

	private BatchJobResponse executeJob(String jobType) {
		return switch (jobType) {
			case "IMPORT_PRODUCT_DATA" -> batchMapper.toResponse(
					jobType,
					"STARTED",
					"Product import started successfully");
			case "GENERATE_DAILY_REPORTS" -> {
				boolean hasData = productRepository.count() > 0 || customerOrderRepository.count() > 0;
				if (!hasData) {
					yield batchMapper.toResponse(jobType, "FAILED", "No data available for daily report generation");
				}
				yield batchMapper.toResponse(jobType, "STARTED", "Daily report generation started successfully");
			}
			case "AGGREGATE_SALES_DATA" -> {
				if (customerOrderRepository.count() == 0) {
					yield batchMapper.toResponse(jobType, "FAILED", "No order data available for sales aggregation");
				}
				yield batchMapper.toResponse(jobType, "STARTED", "Sales aggregation started successfully");
			}
			default -> throw new ValidationException("Unsupported job type");
		};
	}

	private String normalizeJobType(String jobType) {
		if (jobType == null || jobType.trim().isEmpty()) {
			throw new ValidationException("jobType is required");
		}

		String normalized = jobType.trim().toUpperCase(Locale.ROOT);
		if (!SUPPORTED_JOB_TYPES.contains(normalized)) {
			throw new ValidationException("Unsupported job type");
		}

		return normalized;
	}

	private String normalizeCron(String cron) {
		if (cron == null || cron.trim().isEmpty()) {
			throw new ValidationException("cron is required for scheduling");
		}

		return cron.trim();
	}

	private BatchLogEntry findLatestFailedLog(String jobType) {
		List<BatchLogEntry> entries = new ArrayList<>(logs);
		entries.sort((a, b) -> b.createdAt.compareTo(a.createdAt));
		return entries.stream()
				.filter(item -> item.response().getJobType().equals(jobType))
				.filter(item -> "FAILED".equals(item.response().getStatus()))
				.findFirst()
				.orElse(null);
	}

	private void addLog(BatchJobResponse response) {
		logs.add(new BatchLogEntry(LocalDateTime.now(), response));
	}

	private record BatchLogEntry(LocalDateTime createdAt, BatchJobResponse response) {
	}
}
