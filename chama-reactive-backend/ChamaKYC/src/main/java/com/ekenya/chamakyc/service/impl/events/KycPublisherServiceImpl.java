package com.ekenya.chamakyc.service.impl.events;

import com.ekenya.chamakyc.dao.config.AuditLog;
import com.ekenya.chamakyc.service.impl.events.interfaces.PublishingService;
import com.ekenya.chamakyc.wrappers.broker.ChamaContribution;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import static com.ekenya.chamakyc.service.impl.constants.KafkaChannelsConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KycPublisherServiceImpl implements PublishingService {

    private final StreamBridge streamBridge;
    private final Gson gson;

    /**
     * Publish Member info for creation of a Wallet.
     *
     * @param phoneNumber the phone number
     * @param nationalId  the national identification number
     */
    @Override
    public void publishMemberWalletInfo(String phoneNumber, String nationalId) {
        Mono.fromRunnable(() -> {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("phoneNumber", phoneNumber);
            jsonObject.addProperty("nationalId", nationalId);

            streamBridge.send(CREATE_USER_WALLET_TOPIC, jsonObject.toString());
        }).publishOn(Schedulers.boundedElastic()).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    /**
     * Publish info for sending an email.
     *
     * @param title   the subject of the email
     * @param message the information
     * @param email   the recepient's email address
     */
    @Override
    public void sendEmailNotification(String title, String message, String email) {
        Mono.fromRunnable(() -> {
            JsonObject emailDetails = new JsonObject();
            emailDetails.addProperty("title", title);
            emailDetails.addProperty("message", message);
            emailDetails.addProperty("email", email);

            streamBridge.send(SEND_EMAIL_TOPIC, emailDetails.toString());
        }).publishOn(Schedulers.boundedElastic()).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    /**
     * Publish info for sending an OTP SMS.
     *
     * @param smsTypes    the type of OTP
     * @param otpValue    the random integer value
     * @param phoneNumber the recipient's phone number
     * @param language    the language of the text
     */
    @Override
    public void sendOtpSMS(String smsTypes, String otpValue, String phoneNumber, String language) {
        Mono.fromRunnable(() -> {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("type", smsTypes);
            jsonObject.addProperty("otpValue", otpValue);
            jsonObject.addProperty("phoneNumber", phoneNumber);
            jsonObject.addProperty("language", language);

            streamBridge.send(SEND_OTP_TOPIC, jsonObject.toString());
        }).publishOn(Schedulers.boundedElastic()).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    @Override
    public void sendText(String message, String phoneNumber) {
        Mono.fromRunnable(() -> {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("message", message);
            jsonObject.addProperty("phoneNumber", phoneNumber);

            streamBridge.send(SEND_TEXT_TOPIC, jsonObject.toString());
        }).publishOn(Schedulers.boundedElastic()).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    /**
     * Send event to create a Group Contribution.
     *
     * @param contribution contribution details.
     */
    @Override
    public void createGroupContribution(ChamaContribution contribution) {
        Mono.fromRunnable(() -> {
            JsonObject contributionInfo = new JsonObject();
            contributionInfo.addProperty("groupId", contribution.getGroupid());
            contributionInfo.addProperty("contributionName", contribution.getContributionname());
            contributionInfo.addProperty("createdBy", contribution.getCreatedby());
            contributionInfo.addProperty("amountType", contribution.getAmounttypeid());
            contributionInfo.addProperty("scheduleType", contribution.getScheduletypeid());
            contributionInfo.addProperty("penalty", contribution.getPenalty());
            contributionInfo.addProperty("contributionType", contribution.getContributiontypeid());
            contributionInfo.addProperty("contributionDetails", contribution.getContributiondetails().toString());
            contributionInfo.addProperty("initialAmount", contribution.getInitialAmount());

            streamBridge.send(CREATE_CONTRIBUTION_TOPIC, contributionInfo.toString());
        }).publishOn(Schedulers.boundedElastic()).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    @Override
    public void createGroupAccount(long id, String name, String accountNumber, double availableBalance) {
        Mono.fromRunnable(() -> {
            JsonObject accountInfo = new JsonObject();
            accountInfo.addProperty("id", id);
            accountInfo.addProperty("name", name);
            accountInfo.addProperty("availableBalance", availableBalance);
            String acc = accountNumber == null || accountNumber.isBlank() ? "000000000000000" : accountNumber;
            accountInfo.addProperty("accountNumber", acc);

            log.info("Create group account info... {}", accountInfo.toString());
            streamBridge.send(CREATE_GROUP_ACCOUNT_TOPIC, accountInfo.toString());
        }).publishOn(Schedulers.boundedElastic()).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    @Override
    public void enableGroupContributions(long id, String modifier) {
        Mono.fromRunnable(() -> {
            JsonObject groupInfo = new JsonObject();
            groupInfo.addProperty("groupId", id);
            groupInfo.addProperty("modifier", modifier);

            streamBridge.send(ENABLE_GROUP_CONTRIBUTIONS_TOPIC, groupInfo.toString());
        }).publishOn(Schedulers.boundedElastic()).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    @Override
    public void disableGroupContributions(long id, String modifier) {
        Mono.fromRunnable(() -> {
            JsonObject groupInfo = new JsonObject();
            groupInfo.addProperty("groupId", id);
            groupInfo.addProperty("modifier", modifier);

            streamBridge.send(DISABLE_GROUP_CONTRIBUTIONS_TOPIC, groupInfo.toString());
        }).publishOn(Schedulers.boundedElastic()).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    @Override
    public void writeOffLoansAndPenalties(long memberId, long groupId) {
        Mono.fromRunnable(() -> {
            JsonObject memberInfo = new JsonObject();
            memberInfo.addProperty("memberId", memberId);
            memberInfo.addProperty("groupId", groupId);

            streamBridge.send(WRITE_OFF_LOANS_AND_PENALTIES_TOPIC, memberInfo.toString());
        }).publishOn(Schedulers.boundedElastic()).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    @Override
    public void publishAuditLog(AuditLog auditLog) {
        Mono.fromRunnable(() -> {
            log.info("********** Logging request and response **********");
            streamBridge.send(AUDIT_LOG, gson.toJson(auditLog));
        }).publishOn(Schedulers.boundedElastic()).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    @Override
    public void updateContributionName(long groupId, String contributionName, String phoneNumber) {
        Mono.fromRunnable(() -> {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("groupId", groupId);
            jsonObject.addProperty("contributionName", contributionName);
            jsonObject.addProperty("modifiedBy", phoneNumber);

            streamBridge.send(CONTRIBUTION_NAME_EDIT_TOPIC, gson.toJson(jsonObject));
        }).publishOn(Schedulers.boundedElastic()).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    @Override
    public void updateGroupCoreAccount(String account, long groupId, String initialBalance, String modifier) {
        Mono.fromRunnable(() -> {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("groupId", groupId);
            jsonObject.addProperty("account", account);
            jsonObject.addProperty("initialBalance", initialBalance);
            jsonObject.addProperty("modifiedBy", initialBalance);

            streamBridge.send(UPDATE_GROUP_CORE_ACCOUNT, gson.toJson(jsonObject));
        }).publishOn(Schedulers.boundedElastic()).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }
}
