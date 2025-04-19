package com.bootcamp.transaction.repository;

import com.bootcamp.transaction.model.Transaction;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Repository
public interface TransactionRepository extends ReactiveMongoRepository<Transaction, String> {

    Flux<Transaction> findByOrigenProductoId(String productId);
    Flux<Transaction> findByDestinoProductoId(String productId);
    Flux<Transaction> findByOrigenDocument(String customerId);
    Flux<Transaction> findByOrigenProductoIdAndTransactionDateBetween(String productId, LocalDateTime start, LocalDateTime end);
    Flux<Transaction> findByDestinoProductoIdAndTransactionDateBetween(String productId, LocalDateTime start, LocalDateTime end);

}
