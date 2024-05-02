package com.eclectics.chamapayments.repository;

import com.eclectics.chamapayments.model.LoanPenalty;
import com.eclectics.chamapayments.model.LoansDisbursed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;

public interface LoanPenaltyRepository extends JpaRepository<LoanPenalty,Long> {
    List<LoanPenalty> findAllByLoansDisbursed(LoansDisbursed loansDisbursed);
    List<LoanPenalty> findAllByLoansDisbursedAndDueAmountGreaterThan(LoansDisbursed loansDisbursed, Double amount);
    List<LoanPenalty> findAllByMemberId(long memberId);
    Page<LoanPenalty> findAllByMemberId(long memberId, Pageable pageable);
    List<LoanPenalty> findAllByLoansDisbursedAndMemberIdOrderByCreatedOnDesc(LoansDisbursed loansDisbursed,long memberId);
    List<LoanPenalty> findAllByCreatedOnBetweenAndSoftDeleteFalse(Date startDate, Date endDate, Pageable pageable);

    long countByPaymentStatus(String status);

    @Query(nativeQuery = true, value = "SELECT COALESCE(SUM(lp.paid_amount), 0) FROM user_loan_penalty_tbl lp WHERE lp.due_amount = 0")
    long getSumOfSuccessfulRepayments();

    List<LoanPenalty> findAllByMemberIdAndLoansDisbursed(long memberId, LoansDisbursed loanDisbursed);
}
