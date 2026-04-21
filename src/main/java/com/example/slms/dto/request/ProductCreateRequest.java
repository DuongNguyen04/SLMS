package com.example.slms.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductCreateRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.01", message = "price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "stockQuantity is required")
    @PositiveOrZero(message = "stockQuantity must be greater than or equal to 0")
    private Integer stockQuantity;

    @NotBlank(message = "imageUrl is required")
    @Size(max = 500, message = "imageUrl must not exceed 500 characters")
    private String imageUrl;
}
