package com.eclectics.chamapayments.service;

import com.eclectics.chamapayments.model.Accounts;
import com.eclectics.chamapayments.model.Loan;
import com.eclectics.chamapayments.model.LoanApplications;
import com.eclectics.chamapayments.wrappers.request.ApplyLoanWrapper;
import com.eclectics.chamapayments.wrappers.request.LoanRepaymentsWrapper;
import com.eclectics.chamapayments.wrappers.request.PayPenaltyLoanWrapper;
import com.eclectics.chamapayments.wrappers.response.LoanpaymentsWrapper;
import com.eclectics.chamapayments.wrappers.response.LoanproductWrapper;
import com.eclectics.chamapayments.wrappers.response.MemberWrapper;
import com.eclectics.chamapayments.wrappers.response.UniversalResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

import java.util.Date;

/**
 * @author Alex Maina
 * @created 06/12/2021
 */
public interface LoanService {
    int checkLoanLimit(String phoneNumber, Long contributionId);

    boolean checkIfUserHasExistingLoans(String phoneNumber);

    Mono<UniversalResponse> giveUserLoan(Loan loan);

    Mono<UniversalResponse> giveUserGuarantorLoan(Loan loan);

    Mono<UniversalResponse> getGuarantorLoans(String phoneNumber);

    Mono<UniversalResponse> approveDenyGuarantorRequest(Long guarantors, boolean guarantee, String username);

    Mono<UniversalResponse> getUserDeclinedGuarantorLoans(String phoneNumber);

    Mono<UniversalResponse> getLoanGuarantors(Long loanId);

    Mono<UniversalResponse> createLoanProduct(LoanproductWrapper loanproductWrapper, String createdBy);

    Mono<UniversalResponse> editLoanProduct(LoanproductWrapper loanproductWrapper, String approvedBy);

    Mono<UniversalResponse> activateDeactivateLoanProduct(LoanproductWrapper loanproductWrapper, String currentUser, boolean activate);

    Mono<UniversalResponse> getLoanProductsbyGroup(long groupid);

    Mono<UniversalResponse> applyLoan(ApplyLoanWrapper applyLoanWrapper, String username);

    Mono<UniversalResponse> getLoansPendingApprovalbyGroup(long groupid, int page, int size);

    Mono<UniversalResponse> getLoansPendingApprovalbyUser(String phonenumber, int page, int size);

    Mono<UniversalResponse> getLoansPendingApprovalbyLoanProduct(long loanproductid, String currentUser, int page, int size);

    Mono<UniversalResponse> approvedeclineLoanApplication(boolean approve, long loanapplicationid, long debitaccountid, String approvedby);

    Mono<UniversalResponse> approveLoanApplication(boolean approve, long loanApplicationId, String approvedBy);

    Mono<UniversalResponse> getDisbursedLoansperGroup(long groupid, int page, int size);

    void saveWithdrawalLog(LoanApplications loanApplications, Accounts accounts, MemberWrapper loanedMember);

    Mono<UniversalResponse> getDisbursedLoansperLoanproduct(long loanproductid, int page, int size);

    Mono<UniversalResponse> getDisbursedLoansperUser(String phonenumber, int page, int size);

    Mono<UniversalResponse> recordLoanRepayment(long disbursedloanid, double amount, String receiptnumber, FilePart file, String paidby);

    Mono<UniversalResponse> approveLoanRepayment(long loanpaymentid, boolean approve, String approvedby);

    Mono<UniversalResponse> getLoanPaymentPendingApprovalByUser(String phonenumber, int page, int size);

    Mono<UniversalResponse> getLoanPaymentPendingApprovalByGroup(long groupid, String currentUser, int page, int size);

    Mono<UniversalResponse> getLoanPaymentsbyUser(String phonenumber, int page, int size);

    Mono<UniversalResponse> getLoanPaymentsbyGroupid(long groupid, int page, int size);

    Mono<UniversalResponse> getLoanPaymentsbyDisbursedloan(long disbursedloanid, int page, int size);

    Mono<UniversalResponse> getOverdueLoans(long groupid, int page, int size);

    Mono<UniversalResponse> payLoanByMpesa(LoanRepaymentsWrapper loanRepaymentWrapper, String paidBy);

    Mono<UniversalResponse> payLoan(LoanpaymentsWrapper loanpaymentsWrapper);

    Mono<UniversalResponse> getGroupLoansPenalties(Long groupId);

    Mono<UniversalResponse> getMemberLoansPenalties(String phoneNumber);

    Mono<UniversalResponse> getMemberLoansPenalties(String phoneNumber, Integer page, Integer size);

    Mono<UniversalResponse> payLoanPenaltyByMpesa(PayPenaltyLoanWrapper payPenaltyLoanWrapper);

    Mono<UniversalResponse> payLoanPenaltyByReciept(PayPenaltyLoanWrapper payPenaltyLoanWrapper, FilePart file);

    Double getTotalLoansByGroup(long groupId);

    Mono<UniversalResponse> getInactiveGroupLoanProducts(Long groupId);

    Mono<UniversalResponse> getGroupsLoanSummaryPayment(String groupName, Date startDate, Date endDate, Pageable pageable);

    Mono<UniversalResponse> initiateLoanRepayment(LoanRepaymentsWrapper loanRepaymentsWrapper, String username);

    Mono<UniversalResponse> getLoanApplications(Long loanProductId, Integer page, Integer size);

    Mono<UniversalResponse> getUserLoanApplications(String phoneNumber, Integer page, Integer size);

    Mono<UniversalResponse> getLoanPaymentsByLoanProductProduct(long loanProductId, int page, int size);

    Mono<UniversalResponse> getUserLoanProducts(String username);

    Mono<UniversalResponse> getActiveLoanProductsbyGroup(Long groupId, boolean isActive);

    //active loan for ussd
//    Mono<UniversalResponse> getActiveLoanProductsbyGroup(long groupid);
}
