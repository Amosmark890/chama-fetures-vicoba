package com.ekenya.chamakyc.service.impl.events.interfaces;


import com.ekenya.chamakyc.dao.config.AuditLog;
import com.ekenya.chamakyc.wrappers.broker.ChamaContribution;

public interface PublishingService {

    void sendEmailNotification(String title, String message, String email);

    void sendOtpSMS(String smsTypes, String otpValue, String phoneNumber, String language);

    void publishMemberWalletInfo(String phoneNumber, String nationalId);

    void sendText(String message, String phoneNumber);

    void createGroupContribution(ChamaContribution contribution);

    void createGroupAccount(long id, String name, String accountNumber, double availableBalance);

    void enableGroupContributions(long id, String modifier);

    void disableGroupContributions(long id, String modifier);

    void writeOffLoansAndPenalties(long memberId, long groupId);

    void publishAuditLog(AuditLog auditLog);

    void updateContributionName(long groupId, String contributionName, String phoneNumber);

    void updateGroupCoreAccount(String account, long groupId, String initialBalance, String modifier);
}
