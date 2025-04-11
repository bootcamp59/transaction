package com.bootcamp.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommissionResponseReportDTO {
    private String productId;
    private LocalDateTime transactionDate;
    private Double transactionCommission;
}
