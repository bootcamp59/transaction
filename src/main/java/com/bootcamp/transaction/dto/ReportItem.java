package com.bootcamp.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReportItem {
    private String productoId;
    private String productType;
    private String banco;
    private long totalTransactions;
    private BigDecimal totalAmount;
}
