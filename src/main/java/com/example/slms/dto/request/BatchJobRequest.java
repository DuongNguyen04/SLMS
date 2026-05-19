package com.example.slms.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BatchJobRequest {

    @NotBlank(message = "jobType is required")
    private String jobType;

    private String cron;

    private Integer retryCount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reportDate;
}
