package com.bootcamp.transaction.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductCommissionReportDto {
    private String productoId;
    private String productoType;
    private BigDecimal totalCommission;
}
