package com.eclectics.chamapayments.repository;


import com.eclectics.chamapayments.model.Loan;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface ContributionLoanRepository extends JpaRepository<Loan,Long> {

    @Query("SELECT contributionLoan FROM Loan contributionLoan WHERE contributionLoan.transactionRef =:transactionId")
    Optional<Loan> findLoanByTransactionById(@Param("transactionId") String transactionId);

    @Query(value = "SELECT * FROM contribution_loan_tbl where transaction_status = :paymentStatus and timediff(:time,contribution_loan_tbl.created_on) < '00:05:00'",nativeQuery = true)
    List<Loan> findPendingPayments(@Param("paymentStatus") String paymentStatus, @Param("time") Date date);

    @Query("SELECT contributionLoan FROM Loan contributionLoan WHERE contributionLoan.phoneNumber = :phoneNumber AND contributionLoan.loanStatus = 'APRROVED' OR contributionLoan.loanStatus = 'EXPIRED'")
    List<Loan> findUserLoans(@Param("phoneNumber") String phoneNumber);

    List<Loan> findAllByContributionId(long contributionId);

    List<Loan> findAllByCreatedOnBetweenAndSoftDelete(Date startDate, Date endDate, boolean softDelete, Pageable pageable);
}
