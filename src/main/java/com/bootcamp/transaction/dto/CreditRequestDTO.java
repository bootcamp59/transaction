package com.bootcamp.transaction.dto;

import com.bootcamp.transaction.enums.TransactionType;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreditRequestDTO {
    private double monto;
    private TransactionType transactionType;

}
