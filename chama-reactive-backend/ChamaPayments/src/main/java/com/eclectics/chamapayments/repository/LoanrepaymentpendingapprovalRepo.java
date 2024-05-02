package com.eclectics.chamapayments.repository;

import com.eclectics.chamapayments.model.LoanRepaymentPendingApproval;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;


public interface LoanrepaymentpendingapprovalRepo extends JpaRepository<LoanRepaymentPendingApproval,Long> {

    @Query(nativeQuery = true,
            countQuery = "SELECT COUNT(*) FROM " +
                    "loans_repayment_pendingapproval lrpa JOIN loans_disbursed ld " +
                    "ON lrpa.loandisbursed_id=ld.id " +
                    "WHERE ld.groupid=:groupid and lrpa.pending=true",
            value = "SELECT * FROM " +
                    "loans_repayment_pendingapproval lrpa JOIN loans_disbursed ld " +
                    "ON lrpa.loandisbursed_id=ld.id " +
                    "WHERE ld.groupid=:groupid and lrpa.pending=true ORDER BY lrpa.created_on DESC")
    List<LoanRepaymentPendingApproval> findpendingbyGroupid(@Param("groupid") long groupid, Pageable pageable);
    @Query(nativeQuery = true,
            value = "SELECT COUNT(*) FROM " +
                    "loans_repayment_pendingapproval lrpa JOIN loans_disbursed ld " +
                    "ON lrpa.loandisbursed_id=ld.id " +
                    "WHERE ld.groupid=:groupid and lrpa.pending=true and lrpa.payment_type = 'receipt'")
    int countpendingbyGroupid(@Param("groupid") long groupid);

    List<LoanRepaymentPendingApproval> findByMemberIdAndPendingTrueOrderByCreatedOnDesc(long members, Pageable pageable);
    int countByMemberIdAndPendingTrue(long memberId);
    Optional<LoanRepaymentPendingApproval> findByPaymentId(String paymentId);
    @Query(value = "SELECT * FROM loans_repayment_pendingapproval where pending = true and timediff" +
            "(:time,loans_repayment_pendingapproval.created_on) < '00:05:00' ORDER BY created_on DESC",nativeQuery = true)
    List<LoanRepaymentPendingApproval> findPendingPayment(@Param("time") Date date);

}
