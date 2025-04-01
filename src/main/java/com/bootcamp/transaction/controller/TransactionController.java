package com.bootcamp.transaction.controller;

import com.bootcamp.transaction.business.TransactionServiceImpl;
import com.bootcamp.transaction.model.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    @PostMapping
    public Mono<ResponseEntity<Transaction>> create(@RequestBody Transaction transaction){
        return transactionService.create(transaction)
                .map(savedTransaction -> ResponseEntity.status(HttpStatus.CREATED).body(savedTransaction))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }

}
