package com.example.slms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BatchJobRequest {

    @NotBlank(message = "jobType is required")
    private String jobType;

    private Integer retryCount;
}
