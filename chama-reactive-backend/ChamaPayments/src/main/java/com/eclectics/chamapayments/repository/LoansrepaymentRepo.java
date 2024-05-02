package com.eclectics.chamapayments.repository;


import com.eclectics.chamapayments.model.LoansDisbursed;
import com.eclectics.chamapayments.model.LoansRepayment;
import com.eclectics.chamapayments.wrappers.response.LoanRepaymentsProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * interface name: LoansrepaymentRepo
 * Creater: wgicheru
 * Date:4/24/2020
 */
public interface LoansrepaymentRepo extends JpaRepository<LoansRepayment, Long> {

    @Query(nativeQuery = true,
            countQuery = "SELECT COUNT(*) FROM " +
                    "loans_repayment lr JOIN loans_disbursed ld " +
                    "ON lr.loandisbursed_id=ld.id " +
                    "WHERE ld.group_id=:groupid",
            value = "SELECT * FROM " +
                    "loans_repayment lr JOIN loans_disbursed ld " +
                    "ON lr.loandisbursed_id=ld.id " +
                    "WHERE ld.group_id=:groupid ORDER BY lr.created_on DESC")
    List<LoansRepayment> getloanpaymentsbyGroupid(@Param("groupid") long groupid, Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT COUNT(*) FROM " +
                    "loans_repayment lr JOIN loans_disbursed ld " +
                    "ON lr.loandisbursed_id=ld.id " +
                    "WHERE ld.group_id=:groupid")
    int countloanpaymentsbyGroupid(@Param("groupid") long groupid);

    List<LoansRepayment> findByMemberIdOrderByCreatedOnDesc(long memberId, Pageable pageable);

    int countByMemberId(long memberId);

    List<LoansRepayment> findByLoansDisbursedOrderByCreatedOnDesc(LoansDisbursed loansDisbursed, Pageable pageable);

    List<LoansRepayment> findByLoansDisbursedOrderByCreatedOnDesc(LoansDisbursed loansDisbursed);

    int countByLoansDisbursed(LoansDisbursed loansDisbursed);

    List<LoansRepayment> findAllByCreatedOnBetweenAndSoftDeleteFalseOrderByCreatedOnDesc(Date startDate, Date endDate, Pageable pageable);

    @Query(nativeQuery = true,
            countQuery = "select count(lr.id) from loans_repayment lr, loans_disbursed ld where lr.loandisbursed_id= ld.id and ld.group_id=:groupId and lr.created_on between :startDate and :endDate",
            value = "select * from loans_repayment lr, loans_disbursed ld where lr.loandisbursed_id= ld.id and ld.group_id=:groupId and lr.created_on between :startDate and :endDate order by lr.created_on desc"
    )
    List<LoansRepayment> findAllByGroupIdAndCreatedOnBetweenAndSoftDeleteFalseOrderByCreatedOnDesc(@Param("groupId") long groupId, Date startDate, Date endDate, Pageable pageable);

    int countAllByCreatedOnBetweenAndSoftDeleteFalse(Date startDate, Date endDate);

    @Query(value = "SELECT COALESCE(sum(lr.amount), 0) FROM loans_repayment lr", nativeQuery = true)
    Long getSuccessfulRepaymentsAmount();

    Optional<LoansRepayment> findFirstByReceiptnumber(String tranasctionId);

    List<LoansRepayment> findAllByCreatedOnBetweenAndSoftDeleteFalse(Date startDate, Date endDate, Pageable pageable);

    @Query(nativeQuery = true, value = "select lr.receiptnumber as transactionId,\n" +
            "       concat(u.first_name, concat(' ', u.last_name)) as loanee,\n" +
            "      lr.status as status,\n" +
            "      u.phone_number as phoneNumber,\n" +
            "      ceil(ld.principal) as principal,\n" +
            "      ceil(lr.amount) as amount,\n" +
            "      ceil(lr.newamount) as balance,\n" +
            "      lp.id\n" +
            "from loans_repayment lr\n" +
            "         inner join loans_disbursed ld on lr.loandisbursed_id = ld.id\n" +
            "         inner join loan_applications la on la.id = ld.loanapplicationid\n" +
            "         inner join members_tbl m on m.id = ld.member_id\n" +
            "         inner join users u on u.id = m.user_id\n" +
            "         inner join loan_products lp on lp.id = la.loanproductid\n" +
            "where lp.id = :productId order by lr.created_on desc")
    Page<LoanRepaymentsProjection> findAllRepaymentsByLoanProduct(Long productId, Pageable pageable);

    @Query(nativeQuery = true, value = "select count(lr)\n" +
            "from loans_repayment lr\n" +
            "         inner join loans_disbursed ld on lr.loandisbursed_id = ld.id\n" +
            "         inner join loan_applications la on la.id = ld.loanapplicationid\n" +
            "         inner join members_tbl m on m.id = ld.member_id\n" +
            "         inner join users u on u.id = m.user_id\n" +
            "         inner join loan_products lp on lp.id = la.loanproductid\n" +
            "where lp.id = :loanProductId")
    long countByLoanProduct(long loanProductId);

}
