package com.bootcamp.transaction.repository;

import com.bootcamp.transaction.model.Transaction;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Repository
public interface TransactionRepository extends ReactiveMongoRepository<Transaction, String> {

    Flux<Transaction> findByProductId(String productId);
    Flux<Transaction> findByCustomerId(String customerId);
    Flux<Transaction> findByProductIdAndTransactionDateBetween(String productId, LocalDateTime start, LocalDateTime end);

}
