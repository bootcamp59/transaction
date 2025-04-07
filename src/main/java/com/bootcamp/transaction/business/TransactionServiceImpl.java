package com.bootcamp.transaction.business;

import com.bootcamp.transaction.dto.CreditRequestDTO;
import com.bootcamp.transaction.enums.TransactionType;
import com.bootcamp.transaction.model.Transaction;
import com.bootcamp.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
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
                if(item.getType() == TransactionType.RETIRO ){
                    item.setAmount(-item.getAmount());
                }
                if(item.getType() == TransactionType.CONSUMPTION || item.getType() == TransactionType.PAYMENT){
                    var request = new CreditRequestDTO();
                    request.setMonto(transaction.getAmount());
                    request.setTransactionType(transaction.getType());

                    return udpateCreditBalance(request, "http://localhost:8087/api/v1/credit/" + transaction.getProductId() + "/transaction")
                        .flatMap(e -> Mono.just(transaction));
                }

                return udpateAccountBalance(item)
                    .flatMap(e -> Mono.just(transaction));
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

    private Mono<Object> udpateCreditBalance(CreditRequestDTO bodyValue, String url){

        return webClientBuilder.build()
                .post()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(bodyValue)
                .retrieve()
                .bodyToMono(Object.class)
                .onErrorMap( e -> new RuntimeException("error al actualizar deposito"))
                .doOnError(o -> System.out.println("solo logging error"));

    }

    public Mono<Map<String, Object>> getProductMovements(String customerId, String productId) {
        // Verifica si el producto pertenece al cliente: puede ser cuenta o crédito
        Mono<Boolean> isCuenta = webClientBuilder.build()
                .get()
                .uri("http://localhost:8086/api/v1/account/" + productId + "/customer/" + customerId)
                .retrieve()
                .bodyToMono(Boolean.class)
                .onErrorReturn(false);

        Mono<Boolean> isCredito = webClientBuilder.build()
                .get()
                .uri("http://localhost:8087/api/v1/credit/" + productId + "/customer/" + customerId)
                .retrieve()
                .bodyToMono(Boolean.class)
                .onErrorReturn(false);

        Flux<Transaction> movimientos = transactionRepository.findByProductId(productId);

        return Mono.zip(isCuenta, isCredito)
                .flatMap(tuple -> {
                    boolean pertenece = tuple.getT1() || tuple.getT2();
                    if (!pertenece) {
                        return Mono.error(new RuntimeException("El producto no pertenece al cliente"));
                    }

                    return movimientos.collectList() // Convierte el Flux en una lista
                            .map(transactionList -> {
                                Map<String, Object> result = new HashMap<>();
                                result.put("productId", productId);
                                result.put("movements", transactionList); // Movimientos convertidos en lista
                                return result;
                            });
                });
    }
}
