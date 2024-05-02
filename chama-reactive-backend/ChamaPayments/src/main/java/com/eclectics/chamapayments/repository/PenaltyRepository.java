package com.eclectics.chamapayments.repository;

import com.eclectics.chamapayments.model.Penalty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PenaltyRepository extends JpaRepository<Penalty, Long> {
    List<Penalty> findAllByUserId(Long userId);

    List<Penalty> findAllByGroupId(Long userId, Pageable pageable);

    Penalty findByUserIdAndSchedulePaymentId(Long userId, String scheduleId);

    List<Penalty> findPenaltyBySchedulePaymentId(String schedulePaymentId);

    Penalty findByTransactionId(String transactionId);

    Penalty findFirstByIdAndSchedulePaymentId(Long penaltyId, String schedulePaymentId);

    List<Penalty> findByUserIdAndContributionId(Long userId, Long contributionId);
}
