package com.eclectics.chamapayments.service;

import com.eclectics.chamapayments.wrappers.response.UniversalResponse;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.Map;

public interface DashboardValuesService {

    Mono<Map<String, Object>> transactionsData();

    Mono<UniversalResponse> getGroupTransactionsByType(Date startDate, Date endDate, String period, String transactionType, String group, String additional, Pageable pageable);
}
