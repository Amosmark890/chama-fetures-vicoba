package com.eclectics.chamapayments.repository;

import com.eclectics.chamapayments.model.LoanPenaltyPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface LoanPenaltyPaymentRepository extends JpaRepository<LoanPenaltyPayment,Long> {

    Optional<LoanPenaltyPayment> findByTransactionIdOrderByCreatedOnDesc(String transactionId);

    @Query(value = "SELECT * FROM loan_penalty_payment where payment_status = :paymentStatus and timediff(:time,loan_penalty_payment.created_on) < '00:05:00' order by created_on desc",nativeQuery = true)
    List<LoanPenaltyPayment> findPendingPayment(@Param("paymentStatus") String paymentStatus, @Param("time") Date date);

}
