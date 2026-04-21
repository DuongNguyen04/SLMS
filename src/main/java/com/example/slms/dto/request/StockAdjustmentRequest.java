package com.example.slms.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockAdjustmentRequest {

    @NotNull(message = "stockQuantity is required")
    @Min(value = 0, message = "stockQuantity must be greater than or equal to 0")
    private Integer stockQuantity;
}
