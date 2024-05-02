package com.eclectics.chamapayments.repository;

import com.eclectics.chamapayments.model.ContributionPayment;
import com.eclectics.chamapayments.wrappers.response.UserGroupContributions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface ContributionsPaymentRepository extends JpaRepository<ContributionPayment, Long> {

    @Query("SELECT contributionPayment FROM ContributionPayment contributionPayment WHERE contributionPayment.contributionId = :contributionId")
    Optional<ContributionPayment> findContributionPaymentByContributionIdAndOrderByCreatedOnDesc(@Param("contributionId") Long contributionId);

    @Query("SELECT contributionPayment from ContributionPayment contributionPayment WHERE contributionPayment.transactionId = :transactionId")
    Optional<ContributionPayment> findContributionByTransactionId(@Param("transactionId") String transactionId);

    @Query("SELECT contributionPayment from ContributionPayment contributionPayment WHERE contributionPayment.mpesaCheckoutId = :mpesaCheckoutId")
    Optional<ContributionPayment> findContributionByMpesaId(@Param("mpesaCheckoutId") String mpesaCheckoutId);

    @Query(value = "SELECT * FROM contribution_payment where payment_status = :paymentStatus", nativeQuery = true)
    List<ContributionPayment> findPendingContributions(@Param("paymentStatus") String paymentStatus);

    @Query(value = "SELECT * FROM contribution_payment as cp where cp.contribution_id = :contributionId AND cp.payment_status = 'PAYMENT_SUCCESS' AND cp.phone_number = :phoneNumber", nativeQuery = true)
    List<ContributionPayment> findUsersContribution(Long contributionId, String phoneNumber);

    List<ContributionPayment> findByContributionIdAndPhoneNumberAndPaymentStatus(Long contributionId, String phoneNumber, String paymentStatus);

    @Query("SELECT contributionPayment FROM ContributionPayment contributionPayment where contributionPayment.contributionId = :contributionId AND contributionPayment.paymentStatus = :paymentStatus AND contributionPayment.phoneNumber = :phoneNumber AND contributionPayment.createdOn BETWEEN :startDate AND :endDate")
    List<ContributionPayment> findUserContributionsBetween(@Param("contributionId") Long contributionId, @Param("paymentStatus") String paymentStatus, @Param("phoneNumber") String phoneNumber, LocalDate startDate, LocalDate endDate);

    @Query("SELECT contributionPayment FROM ContributionPayment contributionPayment where contributionPayment.contributionId = :contributionId AND contributionPayment.paymentStatus = :paymentStatus AND contributionPayment.phoneNumber = :phoneNumber")
    List<ContributionPayment> findTotalContributions(@Param("contributionId") Long contributionId, @Param("paymentStatus") String paymentStatus, @Param("phoneNumber") String phoneNumber);

    @Query("SELECT  contributionPayment FROM ContributionPayment contributionPayment WHERE contributionPayment.contributionId = :contributionId AND contributionPayment.paymentStatus = :paymentStatus AND contributionPayment.paymentType = 'MPESA'")
    List<ContributionPayment> findMpesaPaidContributions(@Param("contributionId") Long contributionId, @Param("paymentStatus") String paymentStatus);

    @Query("SELECT  contributionPayment FROM ContributionPayment contributionPayment WHERE contributionPayment.contributionId = :contributionId AND contributionPayment.paymentStatus = :paymentStatus")
    List<ContributionPayment> findPaidContributions(@Param("contributionId") Long contributionId, @Param("paymentStatus") String paymentStatus);

    @Query("SELECT  contributionPayment FROM ContributionPayment contributionPayment WHERE contributionPayment.contributionId = :contributionId AND contributionPayment.penaltyId = :penaltyId")
    List<ContributionPayment> findPenaltyContributions(@Param("contributionId") Long contributionId, @Param("penaltyId") Long penaltyId);

    @Query("SELECT  contributionPayment FROM ContributionPayment contributionPayment WHERE contributionPayment.phoneNumber = :phoneNumber AND contributionPayment.schedulePaymentId = :scheduleId AND contributionPayment.paymentStatus = 'PAYMENT_SUCCESS' AND contributionPayment.isPenalty= false ")
    List<ContributionPayment> findPaidScheduledContributions(@Param("phoneNumber") String phoneNumber, @Param("scheduleId") String scheduleId);

    ContributionPayment findContributionPaymentBySchedulePaymentId(String schedulePaymentId);

    @Query("select new com.eclectics.chamapayments.wrappers.response.UserGroupContributions(c.name, cp.amount) from ContributionPayment cp inner join Contributions c on c.id = cp.contributionId where cp.paymentStatus = 'PAYMENT_SUCCESS' and cp.phoneNumber = :phoneNumber")
    List<UserGroupContributions> findUserContributions(@Param("phoneNumber") String phoneNumber);

    List<ContributionPayment> findContributionPaymentByPhoneNumberAndPaymentStatus(String phoneNumber, String paymentStatus);

    List<ContributionPayment> findAllByCreatedOnBetweenAndSoftDeleteFalse(Date startDate, Date endDate, Pageable pageable);

    List<ContributionPayment> findByPhoneNumberAndContributionId(String phoneNumber, long contributionId);

    List<ContributionPayment> findContributionPaymentByPhoneNumber(String phoneNumber);

    Page<ContributionPayment> findContributionPaymentByPhoneNumber(String phoneNumber, Pageable pageable);

    Page<ContributionPayment> findByIdOrderByCreatedOnDesc(long contributionId, Pageable pageable);

    @Query("select sum(cp.amount) from ContributionPayment cp where cp.groupAccountId = :groupId and cp.paymentStatus = 'PAYMENT_SUCCESS'")
    Optional<Integer> calculateTotalContributionsForGroup(long groupId);

    long countByPaymentStatus(String status);

    @Query(nativeQuery = true, value = "SELECT COALESCE(sum(cp.amount), 0) FROM contribution_payment cp WHERE cp.payment_status = 'PAYMENT_SUCCESS'")
    long getTotalSuccessfulContributions();

    @Query("SELECT COALESCE(SUM(cp.amount), 0) FROM ContributionPayment cp WHERE cp.schedulePaymentId = :contributionScheduledId AND cp.phoneNumber = :phoneNumber AND cp.paymentStatus = 'PAYMENT_SUCCESS' ")
    Integer getTotalMemberContributionsForScheduledPayment(String contributionScheduledId, String phoneNumber);

    Page<ContributionPayment> findByContributionIdOrderByCreatedOn(Long contributionId, Pageable pageable);
    Page<ContributionPayment> findByContributionIdOrderByCreatedOnDesc(Long contributionId, Pageable pageable);



    Page<ContributionPayment> findAllByPhoneNumber(String phoneNumber, Pageable pageable);
}
