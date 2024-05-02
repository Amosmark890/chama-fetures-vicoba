package com.eclectics.chamapayments.service;

import com.eclectics.chamapayments.model.ContributionPayment;
import com.eclectics.chamapayments.model.LoanPenaltyPayment;
import com.eclectics.chamapayments.model.LoanRepaymentPendingApproval;
import com.eclectics.chamapayments.wrappers.esbWrappers.TransactionDetails;
import com.google.gson.JsonObject;

import java.util.Map;

/**
 * @author Alex Maina
 * @created 23/12/2021
 */
public interface PaymentUtil {
    String addFTWallet(String phoneNumber,String creditAccount,String debitAccount,double amount);
    Map<String, Object> sendMessage(TransactionDetails message);
    void extractMpesaResponse(JsonObject jsonObject, ContributionPayment contributionPaymentOptional);
    void extractLoanPaymentPenalty(JsonObject jsonObject, LoanPenaltyPayment loanPenaltyPayment);
    void extractLoanPaymentResponse(JsonObject json, LoanRepaymentPendingApproval loanRepaymentPendingApproval);
    void creditUserWalletAccount(String phoneNumber, long contributionId, double amount);
}
