package com.example.slms.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportGenerateRequest {

    @NotBlank(message = "reportType is required")
    private String reportType;

    @NotBlank(message = "exportFormat is required")
    private String exportFormat;

    private LocalDate fromDate;
    private LocalDate toDate;
}
