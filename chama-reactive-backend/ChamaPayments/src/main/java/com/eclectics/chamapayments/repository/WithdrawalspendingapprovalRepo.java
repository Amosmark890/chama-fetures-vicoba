package com.eclectics.chamapayments.repository;


import com.eclectics.chamapayments.model.Contributions;
import com.eclectics.chamapayments.model.WithdrawalsPendingApproval;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WithdrawalspendingapprovalRepo extends JpaRepository<WithdrawalsPendingApproval,Long> {
    Optional<WithdrawalsPendingApproval> findByIdAndPendingTrue(long id);

    List<WithdrawalsPendingApproval> findByContributionAndPendingTrue(Contributions contributions);
    List<WithdrawalsPendingApproval> findByPhonenumberAndPendingTrue(String phonenumber);

    @Query(nativeQuery = true, value = "SELECT * FROM withdrawals_pending_approval wpa JOIN accounts a ON " +
            "wpa.debitaccount_id=a.id WHERE wpa.pending=true AND wpa.group_id=:groupid order by wpa.created_on desc")
    Page<WithdrawalsPendingApproval> findByGroupandPendingTrue(@Param("groupid") long groupid, Pageable pageable);

}
