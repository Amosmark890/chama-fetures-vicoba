package com.eclectics.chamapayments.repository;

import com.eclectics.chamapayments.model.Guarantors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GuarantorsRepository extends JpaRepository<Guarantors,Long> {

    @Query("SELECT guarantors FROM  Guarantors guarantors WHERE guarantors.phoneNumber = :phoneNumber AND guarantors.loanStatus = :loanStatus OR guarantors.loanStatus = :loanStatus1")
    List<Guarantors> findGuarantorsWithExistingLoans(@Param("phoneNumber") String phoneNumber,@Param("loanStatus") String loanStatus,@Param("loanStatus1") String loanStatus1);

    @Query("select guarantors FROM  Guarantors guarantors WHERE guarantors.phoneNumber = :phoneNumber")
    List<Guarantors> findGuarantorsByPhoneNumber(@Param("phoneNumber") String phoneNumber);
    @Query
    List<Guarantors> findGuarantorsByPhoneNumberAndLoanStatusAndSoftDeleteFalse(String phoneNumber, String loanStatus);
    Optional<Guarantors> findGuarantorsByPhoneNumberAndLoanId(String phoneNumber, long loanId);
    Optional<Guarantors> findGuarantorsByPhoneNumberAndLoanIdAndLoanStatus(String phoneNumber, long loanId, String loanStatus);
    @Query("select guarantors FROM  Guarantors guarantors WHERE guarantors.phoneNumber = :phoneNumber AND guarantors.loanId = :loanId")
    List<Guarantors> findGuarantorsByPhoneNumberAndLoanId(@Param("phoneNumber") Long phoneNumber,@Param("loanId") Long loanId);
    @Query("SELECT guarantors FROM Guarantors guarantors WHERE guarantors.loanId = :loanId")
    List<Guarantors> findGuarantorsByLoanId(@Param("loanId") Long loanId);
    List<Guarantors> findAllByLoanIdAndLoanStatus(Long loanId,String loanStatus);
    List<Guarantors> findAllByLoanIdAndPhoneNumber(Long loanId,String phoneNumber);

    @Query(nativeQuery = true, value = "select * from guarantors_tbl gt inner join loan_applications la on gt.loan_id= la.id where la.member_id=:memberId and gt.loan_status=:loanStatus")
    List<Guarantors> findAllByMemberIdAndLoanStatus(long  memberId, String loanStatus);
}
