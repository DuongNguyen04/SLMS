package com.example.slms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockAdjustmentRequest {

    @NotBlank(message = "productName is required")
    private String productName;

    @NotNull(message = "stockQuantity is required")
    private Integer stockQuantity;
}
