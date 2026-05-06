package com.example.slms.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.slms.dto.request.BatchJobRequest;
import com.example.slms.dto.response.BatchJobResponse;
import com.example.slms.entity.BatchJobLog;
import com.example.slms.entity.BatchJobSchedule;
import com.example.slms.entity.CustomerOrder;
import com.example.slms.entity.Product;
import com.example.slms.entity.SalesAggregate;
import com.example.slms.entity.enums.OrderStatus;
import com.example.slms.exception.BusinessException;
import com.example.slms.exception.ValidationException;
import com.example.slms.mapper.BatchMapper;
import com.example.slms.repository.BatchJobLogRepository;
import com.example.slms.repository.BatchJobScheduleRepository;
import com.example.slms.repository.CustomerOrderRepository;
import com.example.slms.repository.ProductRepository;
import com.example.slms.repository.SalesAggregateRepository;
import com.example.slms.service.BatchService;
import com.example.slms.service.ReportService;

import jakarta.annotation.PostConstruct;
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
	private final BatchJobScheduleRepository batchJobScheduleRepository;
	private final BatchJobLogRepository batchJobLogRepository;
	private final SalesAggregateRepository salesAggregateRepository;
	private final ReportService reportService;
	private final TaskScheduler taskScheduler;

	@Value("${slms.batch.import-products-path:storage/imports/products.csv}")
	private String importProductsPath;

	private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

	@PostConstruct
	void initializeSchedules() {
		List<BatchJobSchedule> schedules = batchJobScheduleRepository.findByActiveTrue();
		for (BatchJobSchedule schedule : schedules) {
			scheduleTask(schedule.getJobType(), schedule.getCron());
		}
	}

	@Override
	@Transactional
	public BatchJobResponse scheduleJob(BatchJobRequest request) {
		String jobType = normalizeJobType(request.getJobType());
		String cron = normalizeCron(request.getCron());

		BatchJobSchedule schedule = batchJobScheduleRepository.findByJobType(jobType)
				.orElseGet(BatchJobSchedule::new);
		schedule.setJobType(jobType);
		schedule.setCron(cron);
		schedule.setActive(true);
		batchJobScheduleRepository.save(schedule);

		scheduleTask(jobType, cron);
		BatchJobResponse response = batchMapper.toResponse(jobType, "SCHEDULED", "Job scheduled: " + cron);
		appendLog(response);
		return response;
	}

	@Override
	@Transactional
	public BatchJobResponse runJob(BatchJobRequest request) {
		String jobType = normalizeJobType(request.getJobType());
		BatchJobResponse response = executeJob(jobType);
		appendLog(response);
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

		BatchJobLog latestFailed = batchJobLogRepository
				.findTopByJobTypeAndStatusOrderByCreatedAtDesc(jobType, "FAILED")
				.orElse(null);
		if (latestFailed == null) {
			throw new ValidationException("Retry is not allowed because no failed job execution was found");
		}

		BatchJobResponse response = executeJob(jobType);
		appendLog(response);
		return response;
	}

	@Override
	@Transactional(readOnly = true)
	public Page<BatchJobResponse> listLogs(int page, int size, String jobType) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
		Page<BatchJobLog> logs = (jobType == null || jobType.trim().isEmpty())
				? batchJobLogRepository.findAll(pageable)
				: batchJobLogRepository.findByJobType(normalizeJobType(jobType), pageable);

		if (logs.isEmpty()) {
			throw new BusinessException("No logs found", HttpStatus.NOT_FOUND);
		}

		return logs.map(log -> batchMapper.toResponse(log.getJobType(), log.getStatus(), log.getMessage()));
	}

	private void scheduleTask(String jobType, String cron) {
		ScheduledFuture<?> existing = scheduledTasks.get(jobType);
		if (existing != null) {
			existing.cancel(false);
		}
		ScheduledFuture<?> future = taskScheduler.schedule(
				() -> runScheduledJob(jobType),
				new CronTrigger(cron));
		scheduledTasks.put(jobType, future);
	}

	private void runScheduledJob(String jobType) {
		BatchJobResponse response = executeJob(jobType);
		appendLog(response);
	}

	private BatchJobResponse executeJob(String jobType) {
		try {
			return switch (jobType) {
				case "IMPORT_PRODUCT_DATA" -> importProductData();
				case "GENERATE_DAILY_REPORTS" -> generateDailyReports();
				case "AGGREGATE_SALES_DATA" -> aggregateSalesData();
				default -> throw new ValidationException("Unsupported job type");
			};
		} catch (RuntimeException ex) {
			return batchMapper.toResponse(jobType, "FAILED", ex.getMessage());
		}
	}

	private BatchJobResponse importProductData() {
		Path path = Paths.get(importProductsPath).toAbsolutePath().normalize();
		if (!Files.exists(path)) {
			return batchMapper.toResponse("IMPORT_PRODUCT_DATA", "FAILED",
					"Import file not found: " + path);
		}

		int created = 0;
		int updated = 0;
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
				CSVParser parser = CSVFormat.DEFAULT
						.withFirstRecordAsHeader()
						.withIgnoreEmptyLines()
						.withTrim()
						.parse(reader)) {
			for (CSVRecord record : parser) {
				String name = getCsvValue(record, "name");
				if (name == null || name.trim().isEmpty()) {
					throw new ValidationException("Product name is required");
				}
				BigDecimal price = new BigDecimal(getCsvValue(record, "price"));
				int stock = Integer.parseInt(getCsvValue(record, "stock_quantity", "stockQuantity"));
				String imageUrl = getCsvValue(record, "image_url", "imageUrl");

				if (price.compareTo(BigDecimal.ZERO) <= 0) {
					throw new ValidationException("Product price must be greater than 0");
				}
				if (stock < 0) {
					throw new ValidationException("Product stock must be non-negative");
				}

				Product product = productRepository.findByNameIgnoreCase(name).orElse(null);
				if (product == null) {
					product = Product.builder().name(name.trim()).build();
					created++;
				} else {
					updated++;
				}

				product.setPrice(price);
				product.setStockQuantity(stock);
				if (imageUrl != null && !imageUrl.trim().isEmpty()) {
					product.setImageUrl(imageUrl.trim());
				}

				productRepository.save(product);
			}
		} catch (IOException ex) {
			return batchMapper.toResponse("IMPORT_PRODUCT_DATA", "FAILED", "Import failed: " + ex.getMessage());
		}

		String message = "Import completed. Created: " + created + ", Updated: " + updated;
		return batchMapper.toResponse("IMPORT_PRODUCT_DATA", "COMPLETED", message);
	}

	private String getCsvValue(CSVRecord record, String... keys) {
		for (String key : keys) {
			if (record.isMapped(key)) {
				return record.get(key);
			}
		}
		throw new ValidationException("Missing required CSV column: " + keys[0]);
	}

	private BatchJobResponse generateDailyReports() {
		LocalDate targetDate = LocalDate.now().minusDays(1);
		try {
			reportService.generateSalesReport(targetDate, targetDate, "PDF");
			reportService.generateSalesReport(targetDate, targetDate, "EXCEL");
			reportService.generateInventoryReport(targetDate, targetDate, "PDF");
			reportService.generateInventoryReport(targetDate, targetDate, "EXCEL");
		} catch (RuntimeException ex) {
			return batchMapper.toResponse("GENERATE_DAILY_REPORTS", "FAILED", ex.getMessage());
		}

		return batchMapper.toResponse("GENERATE_DAILY_REPORTS", "COMPLETED",
				"Daily reports generated for " + targetDate);
	}

	private BatchJobResponse aggregateSalesData() {
		LocalDate targetDate = LocalDate.now().minusDays(1);
		LocalDateTime start = targetDate.atStartOfDay();
		LocalDateTime end = targetDate.atTime(23, 59, 59);
		List<CustomerOrder> orders = customerOrderRepository.findByCreatedAtBetween(start, end).stream()
				.filter(order -> order.getStatus() == OrderStatus.CONFIRMED
						|| order.getStatus() == OrderStatus.SHIPPED
						|| order.getStatus() == OrderStatus.DELIVERED)
				.toList();

		if (orders.isEmpty()) {
			return batchMapper.toResponse("AGGREGATE_SALES_DATA", "FAILED",
					"No order data available for sales aggregation");
		}

		BigDecimal totalRevenue = orders.stream()
				.map(CustomerOrder::getTotalPrice)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		SalesAggregate aggregate = salesAggregateRepository.findByReportDate(targetDate)
				.orElseGet(() -> SalesAggregate.builder().reportDate(targetDate).build());
		aggregate.setOrderCount(orders.size());
		aggregate.setTotalRevenue(totalRevenue);
		salesAggregateRepository.save(aggregate);

		return batchMapper.toResponse("AGGREGATE_SALES_DATA", "COMPLETED",
				"Aggregated " + orders.size() + " orders for " + targetDate);
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

	private void appendLog(BatchJobResponse response) {
		BatchJobLog log = BatchJobLog.builder()
				.jobType(response.getJobType())
				.status(response.getStatus())
				.message(response.getMessage())
				.build();
		batchJobLogRepository.save(log);
	}
}
