package com.example.slms.service.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.slms.dto.response.ReportResponse;
import com.example.slms.entity.CustomerOrder;
import com.example.slms.entity.Product;
import com.example.slms.entity.enums.OrderStatus;
import com.example.slms.exception.BusinessException;
import com.example.slms.exception.ValidationException;
import com.example.slms.mapper.ReportMapper;
import com.example.slms.repository.CustomerOrderRepository;
import com.example.slms.repository.ProductRepository;
import com.example.slms.service.ReportService;
import com.example.slms.service.ReportStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

	private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
	private static final DateTimeFormatter ROW_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	private static final String DOWNLOAD_BASE_PATH = "/api/reports/files/";

	private final CustomerOrderRepository customerOrderRepository;
	private final ProductRepository productRepository;
	private final ReportMapper reportMapper;
	private final ReportStorageService reportStorageService;

	@Override
	@Transactional(readOnly = true)
	public ReportResponse generateSalesReport(LocalDate startDate, LocalDate endDate, String format) {
		validateDateRange(startDate, endDate);
		String exportFormat = normalizeFormat(format);

		List<CustomerOrder> orders = fetchOrdersForReport(startDate, endDate);
		List<CustomerOrder> filtered = orders.stream()
				.filter(order -> order.getStatus() != OrderStatus.CANCELLED)
				.toList();

		if (filtered.isEmpty()) {
			throw new BusinessException("No report data found", HttpStatus.NOT_FOUND);
		}

		ReportFileResult fileResult = writeSalesReport(filtered, startDate, endDate, exportFormat);
		return reportMapper.toResponse(
				"SALES",
				exportFormat,
				fileResult.fileName(),
				"READY",
				fileResult.downloadUrl());
	}

	@Override
	@Transactional(readOnly = true)
	public ReportResponse generateInventoryReport(LocalDate startDate, LocalDate endDate, String format) {
		validateDateRange(startDate, endDate);
		String exportFormat = normalizeFormat(format);

		List<Product> products = productRepository.findAll();
		if (products.isEmpty()) {
			throw new BusinessException("No report data found", HttpStatus.NOT_FOUND);
		}

		ReportFileResult fileResult = writeInventoryReport(products, startDate, endDate, exportFormat);
		return reportMapper.toResponse(
				"INVENTORY",
				exportFormat,
				fileResult.fileName(),
				"READY",
				fileResult.downloadUrl());
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

	private List<CustomerOrder> fetchOrdersForReport(LocalDate startDate, LocalDate endDate) {
		if (startDate == null && endDate == null) {
			return customerOrderRepository.findAll();
		}

		LocalDateTime start = startDate == null ? LocalDate.MIN.atStartOfDay() : startDate.atStartOfDay();
		LocalDateTime end = endDate == null
				? LocalDate.MAX.atTime(23, 59, 59)
				: endDate.atTime(23, 59, 59);
		return customerOrderRepository.findByCreatedAtBetween(start, end);
	}

	private ReportFileResult writeSalesReport(
			List<CustomerOrder> orders,
			LocalDate startDate,
			LocalDate endDate,
			String format) {
		String fileName = buildFileName("sales", format);
		Path target = reportStorageService.resolveFile(fileName);
		try {
			if (format.equals("PDF")) {
				writeSalesPdf(target, orders, startDate, endDate);
			} else {
				writeSalesExcel(target, orders, startDate, endDate);
			}
		} catch (IOException ex) {
			throw new BusinessException("Unable to generate sales report", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ReportFileResult(fileName, DOWNLOAD_BASE_PATH + fileName);
	}

	private ReportFileResult writeInventoryReport(
			List<Product> products,
			LocalDate startDate,
			LocalDate endDate,
			String format) {
		String fileName = buildFileName("inventory", format);
		Path target = reportStorageService.resolveFile(fileName);
		try {
			if (format.equals("PDF")) {
				writeInventoryPdf(target, products, startDate, endDate);
			} else {
				writeInventoryExcel(target, products, startDate, endDate);
			}
		} catch (IOException ex) {
			throw new BusinessException("Unable to generate inventory report", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ReportFileResult(fileName, DOWNLOAD_BASE_PATH + fileName);
	}

	private void writeSalesPdf(Path target, List<CustomerOrder> orders, LocalDate startDate, LocalDate endDate)
			throws IOException {
		List<String> lines = new ArrayList<>();
		lines.add(buildDateRangeLine(startDate, endDate));
		lines.add("Order ID | Customer | Total | Status | Created");

		BigDecimal totalRevenue = BigDecimal.ZERO;
		for (CustomerOrder order : orders) {
			totalRevenue = totalRevenue.add(order.getTotalPrice());
			lines.add(
					order.getOrderId() + " | "
							+ order.getCustomerUsername() + " | "
							+ order.getTotalPrice() + " | "
							+ order.getStatus() + " | "
							+ formatCreatedAt(order.getCreatedAt()));
		}

		lines.add("Total orders: " + orders.size());
		lines.add("Total revenue: " + totalRevenue);
		writePdf(target, "Sales Report", lines);
	}

	private void writeInventoryPdf(Path target, List<Product> products, LocalDate startDate, LocalDate endDate)
			throws IOException {
		List<String> lines = new ArrayList<>();
		lines.add(buildDateRangeLine(startDate, endDate));
		lines.add("Product | Price | Stock");
		for (Product product : products) {
			lines.add(product.getName() + " | " + product.getPrice() + " | " + product.getStockQuantity());
		}
		writePdf(target, "Inventory Report", lines);
	}

	private void writePdf(Path target, String title, List<String> lines) throws IOException {
		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage(PDRectangle.LETTER);
			document.addPage(page);
			PDPageContentStream contentStream = new PDPageContentStream(document, page);
			float y = page.getMediaBox().getHeight() - 50;
			contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
			contentStream.beginText();
			contentStream.newLineAtOffset(50, y);
			contentStream.showText(title);
			contentStream.endText();
			y -= 30;
			contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);

			for (String line : lines) {
				if (y < 60) {
					contentStream.close();
					page = new PDPage(PDRectangle.LETTER);
					document.addPage(page);
					contentStream = new PDPageContentStream(document, page);
					y = page.getMediaBox().getHeight() - 50;
					contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
				}
				contentStream.beginText();
				contentStream.newLineAtOffset(50, y);
				contentStream.showText(line);
				contentStream.endText();
				y -= 16;
			}

			contentStream.close();
			document.save(target.toFile());
		}
	}

	private void writeSalesExcel(Path target, List<CustomerOrder> orders, LocalDate startDate, LocalDate endDate)
			throws IOException {
		try (Workbook workbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(target)) {
			Sheet sheet = workbook.createSheet("Sales Report");
			int rowIndex = 0;

			Row titleRow = sheet.createRow(rowIndex++);
			Cell titleCell = titleRow.createCell(0);
			titleCell.setCellValue("Sales Report");

			Row rangeRow = sheet.createRow(rowIndex++);
			rangeRow.createCell(0).setCellValue(buildDateRangeLine(startDate, endDate));

			Row headerRow = sheet.createRow(rowIndex++);
			String[] headers = { "Order ID", "Customer", "Total", "Status", "Created" };
			CellStyle headerStyle = createHeaderStyle(workbook);
			for (int i = 0; i < headers.length; i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(headers[i]);
				cell.setCellStyle(headerStyle);
			}

			BigDecimal totalRevenue = BigDecimal.ZERO;
			for (CustomerOrder order : orders) {
				Row row = sheet.createRow(rowIndex++);
				row.createCell(0).setCellValue(order.getOrderId());
				row.createCell(1).setCellValue(order.getCustomerUsername());
				row.createCell(2).setCellValue(order.getTotalPrice().doubleValue());
				row.createCell(3).setCellValue(order.getStatus().name());
				row.createCell(4).setCellValue(formatCreatedAt(order.getCreatedAt()));
				totalRevenue = totalRevenue.add(order.getTotalPrice());
			}

			Row summaryRow = sheet.createRow(rowIndex++);
			summaryRow.createCell(0).setCellValue("Total orders");
			summaryRow.createCell(1).setCellValue(orders.size());

			Row revenueRow = sheet.createRow(rowIndex++);
			revenueRow.createCell(0).setCellValue("Total revenue");
			revenueRow.createCell(1).setCellValue(totalRevenue.doubleValue());

			for (int i = 0; i < headers.length; i++) {
				sheet.autoSizeColumn(i);
			}

			workbook.write(outputStream);
		}
	}

	private void writeInventoryExcel(Path target, List<Product> products, LocalDate startDate, LocalDate endDate)
			throws IOException {
		try (Workbook workbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(target)) {
			Sheet sheet = workbook.createSheet("Inventory Report");
			int rowIndex = 0;

			Row titleRow = sheet.createRow(rowIndex++);
			titleRow.createCell(0).setCellValue("Inventory Report");

			Row rangeRow = sheet.createRow(rowIndex++);
			rangeRow.createCell(0).setCellValue(buildDateRangeLine(startDate, endDate));

			Row headerRow = sheet.createRow(rowIndex++);
			String[] headers = { "Product", "Price", "Stock" };
			CellStyle headerStyle = createHeaderStyle(workbook);
			for (int i = 0; i < headers.length; i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(headers[i]);
				cell.setCellStyle(headerStyle);
			}

			for (Product product : products) {
				Row row = sheet.createRow(rowIndex++);
				row.createCell(0).setCellValue(product.getName());
				row.createCell(1).setCellValue(product.getPrice().doubleValue());
				row.createCell(2).setCellValue(product.getStockQuantity());
			}

			for (int i = 0; i < headers.length; i++) {
				sheet.autoSizeColumn(i);
			}

			workbook.write(outputStream);
		}
	}

	private CellStyle createHeaderStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		Font font = workbook.createFont();
		font.setBold(true);
		style.setFont(font);
		return style;
	}

	private String buildDateRangeLine(LocalDate startDate, LocalDate endDate) {
		if (startDate == null && endDate == null) {
			return "Date range: all";
		}
		return "Date range: "
				+ (startDate == null ? "any" : startDate)
				+ " -> "
				+ (endDate == null ? "any" : endDate);
	}

	private String formatCreatedAt(LocalDateTime createdAt) {
		if (createdAt == null) {
			return "N/A";
		}
		return createdAt.format(ROW_TIME_FORMAT);
	}

	private record ReportFileResult(String fileName, String downloadUrl) {
		private ReportFileResult {
			Objects.requireNonNull(fileName, "fileName");
			Objects.requireNonNull(downloadUrl, "downloadUrl");
		}
	}
}
