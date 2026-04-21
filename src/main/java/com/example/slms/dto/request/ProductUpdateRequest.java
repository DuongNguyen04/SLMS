package com.example.slms.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductUpdateRequest {

    private String name;

    @DecimalMin(value = "0.01", message = "price must be greater than 0")
    private BigDecimal price;

    @PositiveOrZero(message = "stockQuantity must be greater than or equal to 0")
    private Integer stockQuantity;

    @Size(max = 500, message = "imageUrl must not exceed 500 characters")
    private String imageUrl;
}
