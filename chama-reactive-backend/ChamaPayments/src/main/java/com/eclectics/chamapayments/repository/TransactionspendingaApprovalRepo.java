package com.eclectics.chamapayments.repository;

import com.eclectics.chamapayments.model.Contributions;
import com.eclectics.chamapayments.model.TransactionsPendingApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransactionspendingaApprovalRepo extends JpaRepository<TransactionsPendingApproval, Long> {
    Optional<TransactionsPendingApproval> findByIdAndPendingTrue(long id);

    @Query(nativeQuery = true, value = "SELECT * FROM transactions_pending_approval tpa JOIN accounts a ON " +
            "tpa.creditaccount_id=a.id WHERE tpa.pending=true AND a.group_id=:groupid")
    List<TransactionsPendingApproval> findByGroupandPendingTrue(@Param("groupid") long groupid);

    List<TransactionsPendingApproval> findByPhonenumberAndPendingTrue(String phonenumber);

    List<TransactionsPendingApproval> findByContributionAndPendingTrue(Contributions contributions);
}
