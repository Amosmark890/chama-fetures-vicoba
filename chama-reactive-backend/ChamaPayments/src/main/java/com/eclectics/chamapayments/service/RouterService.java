package com.eclectics.chamapayments.service;

import com.eclectics.chamapayments.model.ContributionPayment;
import com.eclectics.chamapayments.model.LoanPenaltyPayment;
import com.eclectics.chamapayments.model.LoanRepaymentPendingApproval;
import com.google.gson.JsonObject;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface RouterService {
    Mono<Boolean> makeB2Crequest(String phoneNumber, double amount, Long contributionLoanId);
    void makeB2CwithdrawalRequest(String phoneNumber, double amount, String uniqueId);
    String makeStkPushRequest(JsonObject jsonObject);
    void queryMpesaTransactionStatus(String mpesaCheckoutId);
    void queryLoanPenaltyPaymentStatus(LoanPenaltyPayment loanPenaltyPayment);
    void queryLoanPaymentStatus(LoanRepaymentPendingApproval loanRepaymentPendingApproval);
    void queryB2CTransactionStatus(String transactionId, boolean isContribution);

}
