package com.example.slms.service;

import java.time.LocalDate;

import com.example.slms.dto.response.ReportResponse;

public interface ReportService {

	ReportResponse generateSalesReport(LocalDate startDate, LocalDate endDate, String format);

	ReportResponse generateInventoryReport(LocalDate startDate, LocalDate endDate, String format);
}
