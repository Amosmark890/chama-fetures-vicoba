package com.eclectics.chamapayments.repository;

import com.eclectics.chamapayments.model.LoanApplications;
import com.eclectics.chamapayments.model.LoansDisbursed;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;


public interface LoansdisbursedRepo extends JpaRepository<LoansDisbursed, Long> {
    List<LoansDisbursed> findByGroupId(long groups, Pageable pageable);

    int countAllByCreatedOnBetween(Date startDate, Date endDate);

    int countAllByGroupIdAndCreatedOnBetween(long groupId, Date startDate, Date endDate);

    int countByGroupId(long groupId);

    @Query(nativeQuery = true,
//            countQuery = "SELECT COUNT(*) FROM " +
//                    "loans_disbursed ld JOIN loan_applications la " +
//                    "ON ld.loanapplicationid=la.id " +
//                    "WHERE la.loanproductid=:loanproductid",
            value = "SELECT * FROM " +
                    "loans_disbursed ld JOIN loan_applications la " +
                    "ON ld.loanapplicationid=la.id " +
                    "WHERE la.loanproductid=:loanproductid")
    List<LoansDisbursed> findByLoanproduct(@Param("loanproductid") long loanproductid, Pageable pageable);

    @Query(nativeQuery = true,
//            countQuery = "SELECT COUNT(*) FROM " +
//                    "loans_disbursed ld JOIN loan_applications la " +
//                    "ON ld.loanapplicationid=la.id " +
//                    "WHERE la.loanproductid=:loanproductid AND groupid= :groupId",
            value = "SELECT * FROM " +
                    "loans_disbursed ld JOIN loan_applications la " +
                    "ON ld.loanapplicationid=la.id " +
                    "WHERE la.loanproductid=:loanproductid AND groupid= :groupid AND ld.created_on BETWEEN  :startDate AND :endDate ORDER BY ld.created_on DESC")
    List<LoansDisbursed> findByLoanproductAndGroup(@Param("loanproductid") long loanproductid, @Param("groupid") long groupId, Date startDate, Date endDate, Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT COUNT(ld.*) FROM " +
                    "loans_disbursed ld JOIN loan_applications la " +
                    "ON ld.loanapplicationid=la.id " +
                    "WHERE la.loanproductid=:loanproductid")
    int countLoansDisbursedbyLoanproduct(@Param("loanproductid") long loanproductid);

    @Query(nativeQuery = true,
            value = "SELECT COUNT(*) FROM " +
                    "loans_disbursed ld JOIN loan_applications la " +
                    "ON ld.loanapplicationid=la.id " +
                    "WHERE la.loanproductid=:loanproductid AND groupid= :groupId AND la.created_on between :startDate AND :endDate ORDER BY ld.created_on DESC")
    int countLoansDisbursedbyLoanproductAndGroup(@Param("loanproductid") long loanproductid, @Param("groupId") long groupid, Date startDate, Date endDate);

    @Query(nativeQuery = true,
            value = "SELECT COUNT(*) FROM " +
                    "loans_disbursed ld JOIN loan_applications la " +
                    "ON ld.loanapplicationid=la.id " +
                    "WHERE la.loanproductid=:loanproductid AND la.created_on between :startDate AND :endDate ORDER BY ld.created_on DESC")
    int countLoansDisbursedbyLoanproductAndGroup(@Param("loanproductid") long loanproductid, Date startDate, Date endDate);

    List<LoansDisbursed> findByMemberIdOrderByCreatedOnDesc(long memberId, Pageable pageable);

    List<LoansDisbursed> findByMemberIdOrderByCreatedOnDesc(long memberId);

    int countByMemberId(long memberId);

    List<LoansDisbursed> findByGroupIdAndDueamountGreaterThanAndDuedateBetweenOrderByCreatedOnDesc(long groupId, double dueamount, Date startdate, Date enddate, Pageable pageable);

    List<LoansDisbursed> findByGroupIdAndDueamountGreaterThanAndDuedateLessThanOrderByCreatedOnDesc(long groupId, double dueamount, Date today, Pageable pageable);

    int countByDueamountGreaterThanAndDuedateBetween(double dueamount, Date startdate, Date enddate);

    int countByGroupIdAndDueamountGreaterThanAndDuedateLessThan(long groupId, double dueamount, Date today);

    List<LoansDisbursed> findAllByGroupIdOrderByCreatedOnDesc(long groupId);

    @Query("SELECT ld FROM LoansDisbursed ld WHERE ld.dueamount > 0.0 and ld.duedate < :today order by ld.createdOn desc")
    List<LoansDisbursed> findExpiredLoans(@Param("today") Date date);

    @Query("SELECT ld FROM LoansDisbursed  ld WHERE ld.memberId = :memberId AND ld.dueamount > 0.0 order by ld.createdOn desc")
    List<LoansDisbursed> findUserPendingLoans(@Param("memberId") Long memberId);

    @Query(nativeQuery = true, value = "SELECT ld FROM loans_disbursed  ld INNER JOIN loan_applications la ON la.id = ld.loanapplicationid INNER JOIN loan_products lp on la.loanproductid = lp.id WHERE ld.member_id = :memberId AND ld.loanapplicationid = :productId AND ld.dueamount > 0.0 order by ld.created_on desc")
    List<LoansDisbursed> findUserPendingLoansInLoanProduct(Long memberId, Long productId);

    @Query(nativeQuery = true,
//            countQuery = "SELECT COUNT(*) FROM " +
//                    "loans_disbursed ld JOIN loan_applications la " +
//                    "ON ld.loanapplicationid=la.id " +
//                    "WHERE la.loanproductid=:loanproductid AND ld.created_on between :startDate and :endDate",
            value = "SELECT * FROM " +
                    "loans_disbursed ld JOIN loan_applications la " +
                    "ON ld.loanapplicationid=la.id " +
                    "WHERE la.loanproductid=:loanproductid AND ld.created_on between :startDate and :endDate ORDER BY ld.created_on DESC")
    List<LoansDisbursed> findAllByLoanProductId(@Param("loanproductid") long loanProductId, Date startDate, Date endDate, Pageable pageable);

    @Query(nativeQuery = true,
//            countQuery = "SELECT COUNT(*) FROM " +
//                    "loans_disbursed ld JOIN loan_applications la " +
//                    "ON ld.loanapplicationid=la.id " +
//                    "WHERE la.loanproductid=:loanproductid  AND ld.dueamount > 0 AND ld.duedate < CURRENT_DATE() AND ld.created_on between :startDate and :endDate",
            value = "SELECT * FROM " +
                    "loans_disbursed ld JOIN loan_applications la " +
                    "ON ld.loanapplicationid=la.id " +
                    "WHERE la.loanproductid=:loanproductid AND ld.dueamount > 0 AND ld.duedate < current_date AND ld.created_on between :startDate and :endDate ORDER BY ld.created_on DESC")
    List<LoansDisbursed> findAllOverdueByLoanProductId(@Param("loanproductid") long loanProductId, Date startDate, Date endDate, Pageable pageable);

    @Query(nativeQuery = true,
//            countQuery = "SELECT COUNT(*) FROM " +
//                    "loans_disbursed ld JOIN loan_applications la " +
//                    "ON ld.loanapplicationid=la.id " +
//                    "WHERE  ld.dueamount > 0 AND ld.duedate < CURRENT_DATE() AND ld.created_on between :startDate and :endDate",
            value = "SELECT ld.* FROM " +
                    "loans_disbursed ld JOIN loan_applications la " +
                    "ON ld.loanapplicationid=la.id " +
                    "WHERE  ld.dueamount > 0 AND ld.duedate < current_date AND ld.created_on between :startDate and :endDate ORDER BY ld.created_on DESC")
    List<LoansDisbursed> findAllOverdue(Date startDate, Date endDate, Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT COUNT(*) FROM " +
                    "loans_disbursed ld JOIN loan_applications la " +
                    "ON ld.loanapplicationid=la.id " +
                    "WHERE  ld.dueamount > 0 AND ld.duedate < current_date AND ld.created_on between :startDate and :endDate")
    int countAllOverdue(Date startDate, Date endDate);

    @Query(nativeQuery = true,
            value = "SELECT COUNT(*) FROM " +
                    "loans_disbursed ld JOIN loan_applications la " +
                    "ON ld.loanapplicationid=la.id " +
                    "WHERE la.loanproductid=:loanproductid  AND ld.dueamount > 0 AND ld.duedate < current_date AND ld.created_on between :startDate and :endDate")
    int countAllOverdueByLoanProductId(@Param("loanproductid") long loanProductId, Date startDate, Date endDate);

    @Query(nativeQuery = true,
//            countQuery = "SELECT COUNT(*) FROM " +
//                    "loans_disbursed ld JOIN loan_applications la " +
//                    "ON ld.loanapplicationid=la.id " +
//                    "WHERE la.loanproductid=:loanproductid AND groupid=:groupid AND ld.dueamount > 0 AND ld.duedate < CURRENT_DATE() AND ld.created_on between :startDate and :endDate",
            value = "SELECT * FROM " +
                    "loans_disbursed ld JOIN loan_applications la " +
                    "ON ld.loanapplicationid=la.id " +
                    "WHERE la.loanproductid=:loanproductid AND group_id=:groupid AND ld.dueamount > 0 AND ld.duedate < CURRENT_DATE AND ld.created_on between :startDate and :endDate ")
    List<LoansDisbursed> findAllOverdueByLoanProductIdAndGroup(@Param("loanproductid") long loanProductId, @Param("groupid") long groupid, Date startDate, Date endDate, Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT COUNT(*) FROM " +
                    "loans_disbursed ld JOIN loan_applications la " +
                    "ON ld.loanapplicationid=la.id " +
                    "WHERE la.loanproductid=:loanproductid AND group_id=:groupid AND ld.dueamount > 0 AND ld.duedate < CURRENT_DATE() AND ld.created_on between :startDate and :endDate")
    int countAllOverdueByLoanProductIdAndGroup(@Param("loanproductid") long loanProductId, @Param("groupid") long groupid, Date startDate, Date endDate);

    @Query(nativeQuery = true,
//            countQuery = "SELECT COUNT(*) FROM " +
//                    "loans_disbursed ld JOIN loan_applications la " +
//                    "ON ld.loanapplicationid=la.id " +
//                    "WHERE la.loanproductid=:loanproductid AND groupid=:groupid AND ld.dueamount > 0 AND ld.duedate < CURRENT_DATE() AND ld.created_on between :startDate and :endDate",
            value = "SELECT * FROM " +
                    "loans_disbursed ld JOIN loan_applications la " +
                    "ON ld.loanapplicationid=la.id " +
                    "WHERE  group_id=:groupid AND ld.dueamount > 0 AND ld.duedate < CURRENT_DATE AND ld.created_on between :startDate and :endDate ORDER BY ld.created_on DESC")
    List<LoansDisbursed> findAllOverdueByGroup(@Param("groupid") long groupid, Date startDate, Date endDate, Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT COUNT(*) FROM " +
                    "loans_disbursed ld JOIN loan_applications la " +
                    "ON ld.loanapplicationid=la.id " +
                    "WHERE group_id=:groupid AND ld.dueamount > 0 AND ld.duedate < CURRENT_DATE() AND ld.created_on between :startDate and :endDate ORDER BY ld.created_on DESC")
    int countAllOverdueByGroup(@Param("groupid") long groupid, Date startDate, Date endDate);

    List<LoansDisbursed> findAllByCreatedOnBetweenOrderByCreatedOnDesc(Date StartDate, Date endDate, Pageable pageable);

    List<LoansDisbursed> findAllByGroupIdAndCreatedOnBetweenOrderByCreatedOnDesc(long groupId, Date startDate, Date endDate, Pageable pageable);

    List<LoansDisbursed> findByMemberIdAndDueamountIsLessThanOrderByCreatedOnDesc(long memberId, double dueAmount);

    @Query(nativeQuery = true, value = "SELECT COALESCE(sum(ld.principal + ld.interest), 0) FROM loans_disbursed ld")
    long getSumOfTotalLoansDisbursed();

    List<LoansDisbursed> findAllByGroupIdAndMemberId(long id, long memberId);

    Optional<LoansDisbursed> findByLoanApplications(LoanApplications application);
}
