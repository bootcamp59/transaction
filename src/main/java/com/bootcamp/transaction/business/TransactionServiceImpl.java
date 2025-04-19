package com.bootcamp.transaction.business;

import com.bootcamp.transaction.dto.AccountResponseDTO;
import com.bootcamp.transaction.dto.CommissionReportDTO;
import com.bootcamp.transaction.dto.CommissionResponseReportDTO;
import com.bootcamp.transaction.dto.CreditRequestDTO;
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
import java.time.ZoneId;
import java.time.ZoneOffset;
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
        return transactionRepository.findByOrigenProductoId(productId);
    }

    public Flux<Transaction> findByCustomerId(String customerId) {
        return transactionRepository.findByOrigenDocument(customerId);
    }

    public Flux<CommissionResponseReportDTO> getCommissionReport(CommissionReportDTO dto) {
        ZoneId zone = ZoneId.of("America/Lima");

        LocalDateTime start = dto.getFecInicial().atStartOfDay(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime end = dto.getFecFin().plusDays(1).atStartOfDay(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();

        return transactionRepository.findByOrigenProductoIdAndTransactionDateBetween(dto.getProductId(), start, end)
                .map( tra -> CommissionResponseReportDTO.builder()
                        .productId(tra.getOrigen().getProductoId())
                        .transactionDate(tra.getTransactionDate())
                        .transactionCommission(tra.getTransactionCommission())
                        .build()
                );
    }

    public Mono<Transaction> create(Transaction transaction) {
        transaction.setTransactionDate(LocalDateTime.now());
        log.info("Intentando registrar transacion {}", transaction);

        return transactionRepository.save(transaction);

        /*return validar(transaction.getProductId()) // Mono<Account>
            .flatMap(account ->
                transactionRepository.findByCustomerId(transaction.getCustomerId()).count()
                    .flatMap(count -> {
                        if(count >= account.getMaximoTransacionSinComision()){
                            log.info("se sobrepaso el limite de transaciones sin comision, se esta añadiendo: "+ account.getCommissionRate());
                            transaction.setTransactionCommission(transaction.getAmount() * (account.getCommissionRate() != null ? account.getCommissionRate() : 0));
                        }
                        if(!StringUtils.equalsIgnoreCase(transaction.getOrigen().getName(), transaction.getDestino().getName())){
                            log.error("Solo se puede hacer tansferencia del mismo banco");
                            return Mono.error(new RuntimeException("Solo se puede hacer tansferencia del mimo banco"));
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

         */


    }

    public Mono<Void> delete(String id) {
        return transactionRepository.deleteById(id);
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

    public Flux<Transaction> getMovements(String document, String productId){
        return transactionRepository.findByDestinoProductoId(productId);
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

                return transactionRepository.findByOrigenProductoId(productId)
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

    private Mono<AccountResponseDTO> validar(String accountNumber){

        return webClientBuilder.build()
            .get()
            .uri("http://localhost:8086/api/v1/account/account-number/"+accountNumber)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(AccountResponseDTO.class)
            .onErrorMap( e -> new RuntimeException("error al obtener cuenta por medio del nrocuenta"))
            .doOnError(o -> System.out.println("solo logging error"));
    }

}
