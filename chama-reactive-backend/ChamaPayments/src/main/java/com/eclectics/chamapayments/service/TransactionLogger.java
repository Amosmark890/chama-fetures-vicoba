package com.eclectics.chamapayments.service;

import com.eclectics.chamapayments.model.*;

/**
 * @author Alex Maina
 * @created 06/12/2021
 */
public interface TransactionLogger {
    void logContribution(TransactionsLog transactionsLog, Accounts accounts, TransactionsPendingApproval transactionsPendingApproval);
    void logContributionPayment_forApproval(TransactionsPendingApproval transactionsPendingApproval);
    void logWithdrawalRequest_forApproval(WithdrawalsPendingApproval withdrawalsPendingApproval);
    void logWithdrawal(WithdrawalLogs withdrawalLogs, Accounts accounts);

    void logResponsetoDB(String xref,boolean success, String valueOf, String readResponsecode);
}
