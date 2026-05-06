package com.example.slms.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.slms.dto.response.ReportResponse;
import com.example.slms.service.ReportService;
import com.example.slms.service.ReportStorageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports")
@Validated
@RequiredArgsConstructor
public class ReportController {

	private final ReportService reportService;
	private final ReportStorageService reportStorageService;

	@GetMapping("/sales")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ReportResponse> generateSalesReport(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			@RequestParam(required = false, defaultValue = "PDF") String format) {
		ReportResponse response = reportService.generateSalesReport(startDate, endDate, format);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/inventory")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ReportResponse> generateInventoryReport(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			@RequestParam(required = false, defaultValue = "PDF") String format) {
		ReportResponse response = reportService.generateInventoryReport(startDate, endDate, format);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/files/{fileName}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Resource> downloadReport(@PathVariable String fileName) {
		Resource resource = reportStorageService.loadAsResource(fileName);
		String contentType = resolveContentType(fileName);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
				.contentType(MediaType.parseMediaType(contentType))
				.body(resource);
	}

	private String resolveContentType(String fileName) {
		String lower = fileName.toLowerCase();
		if (lower.endsWith(".pdf")) {
			return MediaType.APPLICATION_PDF_VALUE;
		}
		if (lower.endsWith(".xlsx")) {
			return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
		}
		return MediaType.APPLICATION_OCTET_STREAM_VALUE;
	}
}
