package com.eclectics.chamapayments.service;

import com.eclectics.chamapayments.wrappers.response.MemberWrapper;

import java.util.List;

/**
 * @author Alex Maina
 * @created 06/12/2021
 */
public interface NotificationService {
    void sendLoanApprovalText(MemberWrapper member, Double amount, String language);

    void sendMail(String title, String message, String email, String type, String language);

    void sendGuarantorsInviteMessage(List<String> phoneNumbers, Double amount, String loanRecipientName, String language);

    void sendGuarantorshipApprovalMessage(String guarantorPhone, String guarantorName, String applicantPhone, String applicantName, double amount, String language);

    void sendGuarantorshipDeclinedMessage(String guarantorPhone, String guarantorName, String applicantPhone, String applicantName, double amount, String language);

    public void sendGroupGuarantorshipDeclinedMessage(String memberPhone, String guarantorName, String applicantPhone, String applicantName, double amount, String language);

    void sendLoanApprovedMessage(String phonenumber, String name, double amount, String language);

    void sendLoanDeclinedMessage(String phoneNumber, String name, double amount, String language);

    void sendLoanDisbursedMessage(String phoneNumber, String name, double amount, String language);

    void sendWithdrawalRequestDeclineText(String phoneNumber, String name, double amount, String language);

    void sendWithdrawalRequestApprovedText(String phoneNumber, String name, double amount, String language);

    void sendB2cError(String phoneNumber, String name, double amount, String language);

    void sendPenaltyFailureMessage(String phoneNumber, String name, Integer amount, String language);

    void sendPenaltySuccessMessage(String phoneNumber, String name, Integer amount, String language);

    void sendContributionFailureMessage(String phoneNumber, String name, Integer amount, String language);

    void sendContributionSuccessMessage(String phoneNumber, String name, Integer amount, String language);

    void sendContributionSuccessMessageToGroup(String phoneNumber, String memberName, String groupName, Integer amount, String language);

    void sendReminderMessage(MemberWrapper groupMembership, String name, String contributionName, Integer reminder, String language);

    void sendLoanDisbursementTextToGroup(String phoneNumber, String memberName, String groupName, double amount, String language);

    void sendLoanRepaymentSuccessText(String phoneNumber, String memberName, String name, int amountPaid, String language);

    void sendLoanRepaymentFailureText(String phoneNumber, String memberName, String groupName, int amount, String language);

    void sendContributionWithdrawalFailure(String phoneNumber, String firstname, String groupName, int amount, String language);

    void sendContributionWithdrawalSuccess(String phoneNumber, String firstname, String groupName, int amount, String language);

    void sendContributionWithdrawalToGroup(String phoneNumber, String memberName, String groupName, double amount, String language);

    void sendPenaltyCreatedMessage(String phoneNumber, String memberName, String scheduledId, String groupName, double amount, String language);

    void sendPenaltyCreatedMessageToGroup(String phoneNumber, String memberName, String scheduledId, double amount, String language, String groupName);

    void sendOutstandingPaymentConfirmation(String phoneNumber, String firstname, int dueAmount, String groupName, int remainder, String scheduleId, String language);

    void sendGroupGuarantorshipApprovedMessage(String first, String guarantorName, String phoneNumber, String memberName, double amount, String language);

    void sendPenaltySuccessMessageToGroup(String first, String memberName, String groupName, Integer amount, String language);

    void sendMemberWithdrawRequestText(String memberName, double amount, String groupName, String phoneNumber, String language);

    void sendLoanProductCreated(String memberName, String productName, String name, String phoneNumber, String language);

    void sendLoanProductEdited(String memberName, String productName, String groupName, double maxPrincipal, double minPrincipal, int userSavingValue, Integer penaltyValue, String phoneNumber, String language);

    void sendLoanProductActivatedMessage(String memberName, String productName, String groupName, String phoneNumber, String language);

    void sendLoanProductDeactivatedMessage(String memberName, String productName, String groupName, String phoneNumber, String language);

    void sendContributionEditText(String memberName, String contributionName, String groupName, Long contributionAmount, String displayName, String phoneNumber, String language);

    void sendLoanApplicationSms(String firstname, String memberName, String groupName, double amount, String phoneNumber, String language);
}
