package com.bootcamp.transaction.business;

import com.bootcamp.transaction.enums.TransactionType;
import com.bootcamp.transaction.model.Transaction;
import com.bootcamp.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl {

    private final TransactionRepository transactionRepository;
    private final WebClient.Builder webClientBuilder;

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
        return transactionRepository.save(transaction)
                .flatMap(item -> {
                    if(item.getType() == TransactionType.RETIRO){
                        item.setAmount(-item.getAmount());
                    }
                    return udpateAccountBalance(item).flatMap(e -> Mono.just(transaction));
                });
    }

    public Mono<Void> delete(String id) {
        return transactionRepository.deleteById(id);
    }

    private Mono<Object> udpateAccountBalance(Transaction transaction){

        return webClientBuilder.build()
            .put()
            .uri("http://localhost:8086/api/v1/account/"+ transaction.getProductId())
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("balance", transaction.getAmount()))
            .retrieve()
            .bodyToMono(Object.class)
            .onErrorMap( e -> new RuntimeException("error al actualizar deposito"))
            .doOnError(o -> System.out.println("solo logging error"));

    }
}
