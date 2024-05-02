package com.eclectics.chamapayments.service.impl;

import com.eclectics.chamapayments.model.MessageTemplates;
import com.eclectics.chamapayments.repository.MessagetemplatesRepo;
import com.eclectics.chamapayments.service.NotificationService;
import com.eclectics.chamapayments.service.PublishingService;
import com.eclectics.chamapayments.wrappers.response.MemberWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.List;

/**
 * @author Alex Maina
 * @created 07/12/2021
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final PublishingService publishingService;
    private final MessagetemplatesRepo messagetemplatesRepo;
    private final ChamaKycServiceImpl chamaKycService;

    private static NumberFormat numberFormat() {
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setGroupingUsed(true);
        return numberFormat;
    }

    @Override
    public void sendLoanApprovalText(MemberWrapper member, Double amount, String language) {
        MessageTemplates messageTemplates = messagetemplatesRepo.findByTypeAndLanguage("loan_approved", language);
        String message = String.format(messageTemplates.getTemplate(), member.getFirstname(), numberFormat().format(amount));

        publishingService.sendText(message, member.getPhonenumber());
    }

    @Override
    public void sendMail(String title, String message, String email, String type, String language) {
        publishingService.sendEmailNotification(title, message, email);
    }

    @Override
    public void sendGuarantorsInviteMessage(List<String> phoneNumbers, Double amount, String loanRecipientName, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("guarantor_request", language);

        phoneNumbers.parallelStream()
                .forEach(phoneNumber -> {
                    String message = String.format(messageTemplate.getTemplate(), loanRecipientName, amount);
                    publishingService.sendText(message, phoneNumber);
                });
    }

    @Override
    public void sendGuarantorshipApprovalMessage(String guarantorPhone, String guarantorName, String applicantPhone, String applicantName, double amount, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("guarantor_accept_request", language);
        String message = String.format(messageTemplate.getTemplate(), applicantName, guarantorName, numberFormat().format(amount));

        publishingService.sendText(message, guarantorPhone);
    }

    @Override
    public void sendGuarantorshipDeclinedMessage(String guarantorPhone, String guarantorName, String applicantPhone, String applicantName, double amount, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("guarantor_decline_request", language);
        String message = String.format(messageTemplate.getTemplate(), applicantName, guarantorName, numberFormat().format(amount));

        publishingService.sendText(message, guarantorPhone);
    }

    @Override
    public void sendGroupGuarantorshipDeclinedMessage(String memberPhone, String guarantorName, String applicantPhone, String applicantName, double amount, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("guarantor_decline_request_group", language);
        String message = String.format(messageTemplate.getTemplate(), guarantorName, applicantName, numberFormat().format(amount));

        publishingService.sendText(message, memberPhone);
    }

    @Override
    public void sendGroupGuarantorshipApprovedMessage(String memberPhone, String guarantorName, String applicantPhone, String applicantName, double amount, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("guarantor_accept_request_group", language);
        String message = String.format(messageTemplate.getTemplate(), guarantorName, applicantName, numberFormat().format(amount));

        publishingService.sendText(message, memberPhone);
    }

    @Override
    public void sendLoanApprovedMessage(String phoneNumber, String name, double amount, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("loan_approved", language);
        String message = String.format(messageTemplate.getTemplate(), name, numberFormat().format(amount));

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendLoanDeclinedMessage(String phoneNumber, String name, double amount, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("loan_declined", language);
        String message = String.format(messageTemplate.getTemplate(), name, numberFormat().format(amount));

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendLoanDisbursedMessage(String phoneNumber, String name, double amount, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("loan_disbursed", language);
        String message = String.format(messageTemplate.getTemplate(), name, numberFormat().format(amount));

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendWithdrawalRequestDeclineText(String phoneNumber, String name, double amount, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("withdrawal_declined", language);
        String message = String.format(messageTemplate.getTemplate(), name, numberFormat().format(amount));

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendWithdrawalRequestApprovedText(String phoneNumber, String name, double amount, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("withdrawal_approved", language);
        String message = String.format(messageTemplate.getTemplate(), name, numberFormat().format(amount));

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendB2cError(String phoneNumber, String name, double amount, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("", language);
        String message = String.format(messageTemplate.getTemplate(), name, numberFormat().format(amount));

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendPenaltyFailureMessage(String phoneNumber, String name, Integer amount, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("penalty_payment_failure", language);
        String message = String.format(messageTemplate.getTemplate(), name, numberFormat().format(amount));

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendPenaltySuccessMessage(String phoneNumber, String name, Integer amount, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("penalty_payment_success", language);
        String message = String.format(messageTemplate.getTemplate(), name, numberFormat().format(amount));

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendContributionFailureMessage(String phoneNumber, String name, Integer amount, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("contribution_payment_failure", language);
        String message = String.format(messageTemplate.getTemplate(), name, numberFormat().format(amount));

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendContributionSuccessMessage(String phoneNumber, String name, Integer amount, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("contribution_payment_success", language);
        String message = String.format(messageTemplate.getTemplate(), name, numberFormat().format(amount));

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendContributionSuccessMessageToGroup(String phoneNumber, String memberName, String groupName, Integer amount, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("contribution_payment_success_group", language);
        String message = String.format(messageTemplate.getTemplate(), memberName, numberFormat().format(amount), groupName);

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendPenaltySuccessMessageToGroup(String phoneNumber, String memberName, String groupName, Integer amount, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("penalty_payment_success_group", language);
        String message = String.format(messageTemplate.getTemplate(), memberName, numberFormat().format(amount), groupName);

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendReminderMessage(MemberWrapper member, String scheduleType, String contributionName, Integer reminder, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("contribution_payment_reminder", language);
        String message = String.format(messageTemplate.getTemplate(), member.getFirstname(), scheduleType, contributionName, reminder);

        publishingService.sendText(message, member.getPhonenumber());
    }

    @Override
    public void sendLoanDisbursementTextToGroup(String phoneNumber, String memberName, String groupName, double amount, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("loan_disburse_success_group", language);
        String message = String.format(messageTemplate.getTemplate(), memberName, numberFormat().format(amount), groupName);

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendLoanRepaymentSuccessText(String phoneNumber, String memberName, String groupName, int amountPaid, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("loan_repayment_success", language);
        String message = String.format(messageTemplate.getTemplate(), memberName, groupName, numberFormat().format(amountPaid));

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendLoanRepaymentFailureText(String phoneNumber, String memberName, String groupName, int amount, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("loan_repayment_failure", language);
        String message = String.format(messageTemplate.getTemplate(), memberName, numberFormat().format(amount), groupName);

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendContributionWithdrawalFailure(String phoneNumber, String firstname, String groupName, int amount, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("withdrawal_failure", language);
        String message = String.format(messageTemplate.getTemplate(), firstname, numberFormat().format(amount), groupName);

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendContributionWithdrawalSuccess(String phoneNumber, String firstname, String groupName, int amount, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("withdrawal_success", language);
        String message = String.format(messageTemplate.getTemplate(), firstname, numberFormat().format(amount), groupName);

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendContributionWithdrawalToGroup(String phoneNumber, String memberName, String groupName, double amount, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("withdrawal_success_group", language);
        String message = String.format(messageTemplate.getTemplate(), memberName, numberFormat().format(amount), groupName);

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendPenaltyCreatedMessage(String phoneNumber, String memberName, String scheduledId, String groupName, double amount, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("penalty_created", language);
        String message = String.format(messageTemplate.getTemplate(), memberName, numberFormat().format(amount), groupName, scheduledId);

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendPenaltyCreatedMessageToGroup(String phoneNumber, String memberName, String scheduledId, double amount, String language, String groupName) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("penalty_created_group", language);
        String message = String.format(messageTemplate.getTemplate(), memberName, numberFormat().format(amount), groupName, scheduledId);

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendOutstandingPaymentConfirmation(String phoneNumber, String firstname, int dueAmount, String groupName, int remainder, String scheduledId, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("outsanding_contribution_payment", language);
        String message = String.format(messageTemplate.getTemplate(), firstname, numberFormat().format(dueAmount), groupName, scheduledId, remainder);

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendMemberWithdrawRequestText(String memberName, double amount, String groupName, String phoneNumber, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("member_withdraw_request", language);
        String message = String.format(messageTemplate.getTemplate(), memberName, numberFormat().format(amount), groupName);

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendLoanProductCreated(String memberName, String productName, String groupName, String phoneNumber, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("loan_product_created", language);
        String message = String.format(messageTemplate.getTemplate(), memberName, productName, groupName);

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendLoanProductEdited(String memberName, String productName, String groupName, double maxPrincipal,
                                      double minPrincipal, int userSavingValue, Integer penaltyValue, String phoneNumber, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("loan_product_updated", language);
        String message = String.format(messageTemplate.getTemplate(), memberName, productName, groupName, maxPrincipal, minPrincipal, userSavingValue, penaltyValue);

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendLoanProductActivatedMessage(String memberName, String productName, String groupName, String phoneNumber, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("loan_product_activated", language);
        String message = String.format(messageTemplate.getTemplate(), memberName, productName, groupName);

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendLoanProductDeactivatedMessage(String memberName, String productName, String groupName, String phoneNumber, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("loan_product_deactivated", language);
        String message = String.format(messageTemplate.getTemplate(), memberName, productName, groupName);

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendContributionEditText(String memberName, String contributionName, String groupName, Long contributionAmount, String displayName, String phoneNumber, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("contribution_updated", language);
        String message = String.format(messageTemplate.getTemplate(), memberName, contributionName, groupName, contributionAmount, displayName);

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendLoanApplicationSms(String firstname, String memberName, String groupName, double amount, String phoneNumber, String language) {
        MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("loan_applied", language);
        String message = String.format(messageTemplate.getTemplate(), firstname, memberName, amount, groupName);

        publishingService.sendText(message, phoneNumber);
    }
}
