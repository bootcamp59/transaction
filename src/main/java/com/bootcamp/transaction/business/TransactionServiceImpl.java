package com.bootcamp.transaction.business;

import com.bootcamp.transaction.model.Transaction;
import com.bootcamp.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl {

    private final TransactionRepository transactionRepository;

    public Flux<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    public Flux<Transaction> findByProductId(String productId) {
        return transactionRepository.findByProductId(productId);
    }

    public Flux<Transaction> findByCustomerId(String customerId) {
        return transactionRepository.findByCustomerId(customerId);
    }

    public Mono<Transaction> create(Transaction transaction) {
        transaction.setTransactionDate(LocalDateTime.now());
        return transactionRepository.save(transaction);
    }

    public Mono<Void> delete(String id) {
        return transactionRepository.deleteById(id);
    }
}
