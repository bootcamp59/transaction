package com.bootcamp.transaction.model;


import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountBank {
    private String productoId;
    private String document;
    private String banco;
    private String type;
    private double transaccionesSinComision;




}
