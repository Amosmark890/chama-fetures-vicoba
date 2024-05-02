package com.ekenya.chamakyc.service.impl;

import com.ekenya.chamakyc.dao.config.MessageTemplates;
import com.ekenya.chamakyc.repository.config.MessagetemplatesRepo;
import com.ekenya.chamakyc.service.Interfaces.NotificationService;
import com.ekenya.chamakyc.service.impl.constants.SMS_TYPES;
import com.ekenya.chamakyc.service.impl.events.interfaces.PublishingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Alex Maina
 * @created 06/01/2022
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final PublishingService publishingService;
    private final MessagetemplatesRepo messagetemplatesRepo;

    @Override
    public void sendEmail(String title, String message, String email) {
        publishingService.sendEmailNotification(title, message, email);
    }

    @Override
    public void sendPushNotification(String channelId, String name, String message) {
        // TODO: understand why this is here
    }

    @Override
    public void sendInviteSms(SMS_TYPES smsType, String groupName, String phoneNumber, String language) {
        MessageTemplates messageTemplate;
        String message;
        switch (smsType) {
            case NEW_MEMBER_INVITE:
                messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("newmember_invitesms", language);
                message = String.format(messageTemplate.getTemplate(), groupName);
                publishingService.sendText(message, phoneNumber);
                break;
            case EXISTING_MEMBER_INVITE:
                messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("existingmember_invitesms", language);
                message = String.format(messageTemplate.getTemplate(), groupName);
                publishingService.sendText(message, phoneNumber);
                break;
            default:
                break;
        }
    }

    @Override
    public void sendOtpSms(SMS_TYPES sms_types, String name, String otpValue, String phoneNumber, String language) {
        MessageTemplates messageTemplate;
        String message;
        switch (sms_types) {
            case REGISTRATION:
                messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("first_time_login", language);
                message = String.format(messageTemplate.getTemplate(), name, otpValue, otpValue);
                publishingService.sendText(message, phoneNumber);
                break;
            case FORGOTPASSWORD:
                messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("otpsms", language);
                message = String.format(messageTemplate.getTemplate(), name, otpValue, otpValue);
                publishingService.sendText(message, phoneNumber);
                break;
            case DEVICEVERIFICATION:
                messageTemplate = messagetemplatesRepo.findByTypeAndLanguage("device_registration", language);
                message = String.format(messageTemplate.getTemplate(), name, otpValue, otpValue);
                publishingService.sendText(message, phoneNumber);
                break;
            default:
                log.info("The type {} OTP is invalid...", sms_types);
                break;
        }
    }

    @Override
    public void sendRequestToLeaveAccepted(String requestingToLeaveMemberName, String officialName, String phoneNumber, String groupName, String language) {
        MessageTemplates messageTemplates = messagetemplatesRepo.findByTypeAndLanguage("request_to_leave_approved", language);
        String message = String.format(messageTemplates.getTemplate(), requestingToLeaveMemberName, groupName, officialName);

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendRequestToLeaveAcceptedGroup(String requestingToLeaveMemberName, String officialName, String phoneNumber, String groupName, String language) {
        MessageTemplates messageTemplates = messagetemplatesRepo.findByTypeAndLanguage("request_to_leave_approve_group", language);
        String message = String.format(messageTemplates.getTemplate(), requestingToLeaveMemberName, officialName, groupName);

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendRequestToLeaveDeclined(String requestingToLeaveMemberName, String officialName, String phoneNumber, String groupName, String language) {
        MessageTemplates messageTemplates = messagetemplatesRepo.findByTypeAndLanguage("request_to_leave_decline", language);
        String message = String.format(messageTemplates.getTemplate(), requestingToLeaveMemberName, groupName, officialName);

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendRequestToLeaveDeclinedGroup(String requestingToLeaveMemberName, String officialName, String phoneNumber, String groupName, String language) {
        MessageTemplates messageTemplates = messagetemplatesRepo.findByTypeAndLanguage("request_to_leave_decline_group", language);
        String message = String.format(messageTemplates.getTemplate(), requestingToLeaveMemberName, groupName, officialName);

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendNewLeaderElectedMessage(String phoneNumber, String newLeaderName, String groupTitle, String groupName, String language) {
        MessageTemplates messageTemplates = messagetemplatesRepo.findByTypeAndLanguage("new_leader_elect", language);
        String message = String.format(messageTemplates.getTemplate(), newLeaderName, groupTitle, groupName);

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendGroupEnabledSms(String groupName, String phoneNumber, String language) {
        MessageTemplates messageTemplates = messagetemplatesRepo.findByTypeAndLanguage("group_enabled", language);
        String message = String.format(messageTemplates.getTemplate(), groupName);

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendGroupDisabledSms(String groupName, String phoneNumber, String language) {
        MessageTemplates messageTemplates = messagetemplatesRepo.findByTypeAndLanguage("group_disabled", language);
        String message = String.format(messageTemplates.getTemplate(), groupName);

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendGroupInviteNotification(String memberName, String memberPhoneNumber, String groupName, String phoneNumber, String language) {
        MessageTemplates messageTemplates = messagetemplatesRepo.findByTypeAndLanguage("member_invited", language);
        String message = String.format(messageTemplates.getTemplate(), memberName, memberPhoneNumber, groupName);

        publishingService.sendText(message, phoneNumber);
    }

    @Override
    public void sendMemberGroupRejoinSms(String groupName, String phoneNumber, String language) {
        MessageTemplates messageTemplates = messagetemplatesRepo.findByTypeAndLanguage("member_re_invited", language);
        String message = String.format(messageTemplates.getTemplate(), groupName);

        publishingService.sendText(message, phoneNumber);
    }
}
