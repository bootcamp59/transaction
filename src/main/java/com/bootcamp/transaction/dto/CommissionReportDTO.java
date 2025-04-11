package com.bootcamp.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.repository.NoRepositoryBean;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoRepositoryBean
public class CommissionReportDTO {
    private String productId;
    private LocalDate fecInicial;
    private LocalDate fecFin;
}
