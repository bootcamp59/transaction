package com.bootcamp.transaction.repository;

import com.bootcamp.transaction.model.Transaction;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface TransactionRepository extends ReactiveMongoRepository<Transaction, String> {

    Flux<Transaction> findByProductId(String productId);
    Flux<Transaction> findByCustomerId(String customerId);
}
