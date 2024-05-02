package com.eclectics.chamapayments.repository;

import com.eclectics.chamapayments.model.EsbRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EsbRequestLogRepository extends JpaRepository<EsbRequestLog, Long> {
    Optional<EsbRequestLog> findFirstByField37(String transactionId);
}