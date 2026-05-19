package com.example.slms.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesSummaryResponse {

    private LocalDate reportDate;
    private Integer orderCount;
    private BigDecimal totalRevenue;
}
