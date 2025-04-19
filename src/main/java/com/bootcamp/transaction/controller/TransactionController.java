package com.bootcamp.transaction.controller;

import com.bootcamp.transaction.business.TransactionServiceImpl;
import com.bootcamp.transaction.dto.CommissionReportDTO;
import com.bootcamp.transaction.dto.CommissionResponseReportDTO;
import com.bootcamp.transaction.dto.ProductCommissionReportDto;
import com.bootcamp.transaction.dto.ReporteCommisionRequest;
import com.bootcamp.transaction.model.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/transaction")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionServiceImpl transactionService;

    @GetMapping
    public Flux<Transaction> findAll(){
        return transactionService.findAll();
    }

    @GetMapping("/product/{id}")
    public Flux<Transaction> findByProductId(@PathVariable String id){
        return transactionService.findByProductId(id);
    }

    @GetMapping("/customer/{id}")
    public Flux<Transaction> findByCustomerId(@PathVariable String id){
        return transactionService.findByCustomerId(id);
    }

    @PostMapping("/report")
    public Flux<CommissionResponseReportDTO> reportByProductoId(@RequestBody CommissionReportDTO dto){
        return transactionService.getCommissionReport(dto);
    }

    //este es el verdadero
    @PostMapping("/report-commission")
    public Mono<List<ProductCommissionReportDto>> reportCommission(@RequestBody ReporteCommisionRequest dto){
        return transactionService.generateCommissionReport(dto.getStartDate(), dto.getEndDate());
    }

    @PostMapping
    public Mono<ResponseEntity<Transaction>> create(@RequestBody Transaction transaction){
        return transactionService.create(transaction)
                .map(savedTransaction -> ResponseEntity.status(HttpStatus.CREATED).body(savedTransaction));
    }

    @GetMapping("/customer/{customerId}/product/{productId}")
    public Mono<Map<String, Object>> findByProductIdAndCustomer(@PathVariable String customerId, @PathVariable String productId) {
        return transactionService.getProductMovements(customerId, productId);
    }

    @GetMapping("/customer/{customerId}/product/{productId}/v2")
    public Flux<Transaction> findByProductIdAndCustomer2(@PathVariable String customerId, @PathVariable String productId) {
        return transactionService.getMovements(customerId, productId);
    }

}
