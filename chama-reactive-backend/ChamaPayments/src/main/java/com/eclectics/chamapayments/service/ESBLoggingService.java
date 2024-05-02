package com.eclectics.chamapayments.service;

import com.eclectics.chamapayments.model.EsbRequestLog;
import org.springframework.data.jpa.repository.Query;

import java.util.Map;
import java.util.Optional;

public interface ESBLoggingService {

    void logESBRequest(Map<String, String> body);

    Optional<EsbRequestLog> findByTransactionId(String transactionId);

    EsbRequestLog updateCallbackReceived(EsbRequestLog esbLog);
}
