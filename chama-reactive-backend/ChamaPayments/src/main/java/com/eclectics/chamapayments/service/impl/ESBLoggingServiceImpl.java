package com.eclectics.chamapayments.service.impl;

import com.eclectics.chamapayments.model.EsbRequestLog;
import com.eclectics.chamapayments.repository.EsbRequestLogRepository;
import com.eclectics.chamapayments.service.ESBLoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ESBLoggingServiceImpl implements ESBLoggingService {

    private final EsbRequestLogRepository esbRequestLogRepository;

    @Override
    public void logESBRequest(Map<String, String> body) {
        Mono.fromRunnable(() -> {
            EsbRequestLog esbRequestLog = EsbRequestLog.builder()
                    .field0(body.getOrDefault("0", null))
                    .field2(body.getOrDefault("2", null))
                    .field3(body.getOrDefault("3", null))
                    .field4(body.getOrDefault("4", null))
                    .field24(body.getOrDefault("24", null))
                    .field25(body.getOrDefault("25", null))
                    .field32(body.getOrDefault("32", null))
                    .field37(body.getOrDefault("37", null))
                    .field61(body.getOrDefault("61", null))
                    .field65(body.getOrDefault("65", null))
                    .field66(body.getOrDefault("66", null))
                    .field68(body.getOrDefault("68", null))
                    .field102(body.getOrDefault("102", null))
                    .field103(body.getOrDefault("103", null))
                    .build();

            esbRequestLogRepository.save(esbRequestLog);
        }).subscribeOn(Schedulers.boundedElastic()).publishOn(Schedulers.boundedElastic()).subscribe();
    }

    @Override
    public Optional<EsbRequestLog> findByTransactionId(String transactionId) {
        return esbRequestLogRepository.findFirstByField37(transactionId);
    }

    @Override
    public EsbRequestLog updateCallbackReceived(EsbRequestLog esbLog) {
        esbLog.setCallbackReceived(true);
        return esbRequestLogRepository.save(esbLog);
    }
}
