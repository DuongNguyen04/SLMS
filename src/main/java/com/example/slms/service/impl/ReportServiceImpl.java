package com.example.slms.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.slms.dto.response.ReportResponse;
import com.example.slms.exception.BusinessException;
import com.example.slms.exception.ValidationException;
import com.example.slms.mapper.ReportMapper;
import com.example.slms.repository.CustomerOrderRepository;
import com.example.slms.repository.ProductRepository;
import com.example.slms.service.ReportService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

	private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

	private final CustomerOrderRepository customerOrderRepository;
	private final ProductRepository productRepository;
	private final ReportMapper reportMapper;

	@Override
	@Transactional(readOnly = true)
	public ReportResponse generateSalesReport(LocalDate startDate, LocalDate endDate, String format) {
		validateDateRange(startDate, endDate);
		String exportFormat = normalizeFormat(format);

		if (customerOrderRepository.count() == 0) {
			throw new BusinessException("No report data found", HttpStatus.NOT_FOUND);
		}

		String fileName = buildFileName("sales", exportFormat);
		return reportMapper.toResponse("SALES", exportFormat, fileName, "READY");
	}

	@Override
	@Transactional(readOnly = true)
	public ReportResponse generateInventoryReport(LocalDate startDate, LocalDate endDate, String format) {
		validateDateRange(startDate, endDate);
		String exportFormat = normalizeFormat(format);

		if (productRepository.count() == 0) {
			throw new BusinessException("No report data found", HttpStatus.NOT_FOUND);
		}

		String fileName = buildFileName("inventory", exportFormat);
		return reportMapper.toResponse("INVENTORY", exportFormat, fileName, "READY");
	}

	private void validateDateRange(LocalDate startDate, LocalDate endDate) {
		if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
			throw new ValidationException("startDate must be before or equal to endDate");
		}
	}

	private String normalizeFormat(String format) {
		if (format == null || format.trim().isEmpty()) {
			return "PDF";
		}

		String normalized = format.trim().toUpperCase(Locale.ROOT);
		if (!normalized.equals("PDF") && !normalized.equals("EXCEL")) {
			throw new ValidationException("Unsupported export format. Allowed values: PDF, EXCEL");
		}

		return normalized;
	}

	private String buildFileName(String reportType, String format) {
		String extension = format.equals("PDF") ? "pdf" : "xlsx";
		String timestamp = LocalDateTime.now().format(FILE_TIME_FORMAT);
		return reportType + "_" + timestamp + "." + extension;
	}
}
