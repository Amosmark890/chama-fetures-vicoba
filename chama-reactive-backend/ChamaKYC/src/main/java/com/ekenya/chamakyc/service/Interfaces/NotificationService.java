package com.ekenya.chamakyc.service.Interfaces;

import com.ekenya.chamakyc.service.impl.constants.SMS_TYPES;

/**
 * @author Alex Maina
 * @created 11/12/2021
 */
public interface NotificationService {
    void sendEmail(String title, String message, String email);

    void sendPushNotification(String channelId, String name, String message);

    void sendOtpSms(SMS_TYPES sms_types, String name, String OtpValue, String phoneNumber, String language);

    void sendInviteSms(SMS_TYPES texttype, String name, String phonenumber, String english);

    void sendRequestToLeaveAccepted(String requestingToLeaveMemberName, String officialName, String phoneNumber, String groupName, String language);

    void sendRequestToLeaveAcceptedGroup(String requestingToLeaveMemberName, String officialName, String phoneNumber, String groupName, String language);

    void sendRequestToLeaveDeclined(String requestingToLeaveMemberName, String officialName, String phoneNumber, String groupName, String language);

    void sendRequestToLeaveDeclinedGroup(String requestingToLeaveMemberName, String officialName, String phoneNumber, String groupName, String language);

    void sendGroupEnabledSms(String name, String phoneNumber, String language);

    void sendNewLeaderElectedMessage(String phoneNumber, String newLeaderName, String groupTitle, String groupName, String language);

    void sendGroupDisabledSms(String groupName, String phoneNumber, String language);

    void sendGroupInviteNotification(String memberName, String memberPhoneNumber, String groupName, String language, String s);

    void sendMemberGroupRejoinSms(String groupName, String phoneNumber, String language);
}