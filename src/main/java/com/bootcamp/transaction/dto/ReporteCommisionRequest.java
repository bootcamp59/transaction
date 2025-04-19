package com.bootcamp.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReporteCommisionRequest {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
