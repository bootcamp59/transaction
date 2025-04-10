package com.bootcamp.transaction.business;

import com.bootcamp.transaction.dto.AccountResponseDTO;
import com.bootcamp.transaction.dto.CreditRequestDTO;
import com.bootcamp.transaction.enums.TransactionType;
import com.bootcamp.transaction.model.Transaction;
import com.bootcamp.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
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
        log.info("Intentando registrar transacion {}", transaction);

        return validar(transaction.getProductId()) // Mono<Account>
            .flatMap(account ->
                transactionRepository.findByCustomerId(transaction.getCustomerId()).count()
                    .flatMap(count -> {
                        if(count >= account.getMaximoTransacionSinComision()){
                            log.info("se sobrepaso el limite de transaciones sin comision, se esta añadiendo: "+ account.getCommissionRate());
                            transaction.setTransactionCommission(transaction.getAmount() * account.getCommissionRate());
                        }
                        return transactionRepository.save(transaction)
                            .flatMap(savedTx -> {
                                // Ajuste de monto si es retiro
                                if (transaction.getType() == TransactionType.RETIRO) {
                                    transaction.setAmount(-transaction.getAmount());
                                }
                                if (savedTx.getType() == TransactionType.CONSUMPTION || savedTx.getType() == TransactionType.PAYMENT) {
                                    var request = new CreditRequestDTO();
                                    request.setMonto(savedTx.getAmount());
                                    request.setTransactionType(savedTx.getType());

                                    return udpateCreditBalance(request,
                                            "http://localhost:8087/api/v1/credit/" + savedTx.getProductId() + "/transaction")
                                            .thenReturn(savedTx);
                                } else {
                                    return udpateAccountBalance(savedTx)
                                            .thenReturn(savedTx);
                                }
                            });
                    })
            );


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
        log.info("buscando movimientos del cliente {} con el producto {}",  customerId, productId);
        var urlAccount = "http://localhost:8086/api/v1/account/" + productId + "/customer/" + customerId;
        var urlCredit = "http://localhost:8087/api/v1/credit/" + productId + "/customer/" + customerId;

        log.info("peticion hacia {}", urlAccount);
        Mono<Boolean> isCuenta = webClientBuilder.build()
                .get()
                .uri(urlAccount)
                .retrieve()
                .bodyToMono(Boolean.class)
                .doOnNext(f -> System.out.println("¿Es cuenta?: " + f))
                .onErrorReturn(false);

        log.info("peticion hacia {}", urlAccount);
        Mono<Boolean> isCredito = webClientBuilder.build()
                .get()
                .uri(urlCredit)
                .retrieve()
                .bodyToMono(Boolean.class)
                .doOnNext(f -> System.out.println("¿Es crédito?: " + f))
                .onErrorReturn(false);

        return Mono.zip(isCuenta, isCredito)
            .doOnNext(t -> System.out.println("Resultado de zip: cuenta=" + t.getT1() + ", credito=" + t.getT2()))
            .flatMap(tuple -> {
                boolean pertenece = tuple.getT1() || tuple.getT2();
                System.out.println("¿Pertenece?: " + pertenece);

                if (!pertenece) {
                    return Mono.error(new RuntimeException("El producto no pertenece al cliente"));
                }

                return transactionRepository.findByProductId(productId)
                        .doOnSubscribe(s -> System.out.println("Suscribiéndose a movimientos"))
                        .doOnNext(tx -> System.out.println("Movimiento encontrado: " + tx))
                        .doOnComplete(() -> System.out.println("Todos los movimientos fueron emitidos"))
                        .collectList()
                        .map(transactionList -> {
                            Map<String, Object> result = new HashMap<>();
                            result.put("productId", productId);
                            result.put("movements", transactionList);
                            return result;
                        });
            });
    }

    private Mono<AccountResponseDTO> validar(String cuentaId){

        return webClientBuilder.build()
            .get()
            .uri("http://localhost:8086/api/v1/account/"+cuentaId)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(AccountResponseDTO.class)
            .onErrorMap( e -> new RuntimeException("error al actualizar deposito"))
            .doOnError(o -> System.out.println("solo logging error"));
    }

}
