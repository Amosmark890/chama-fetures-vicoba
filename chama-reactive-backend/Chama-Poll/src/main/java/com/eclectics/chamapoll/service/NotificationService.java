package com.eclectics.chamapoll.service;

import java.util.Date;

public interface NotificationService {

    void sendDeleteGroupMessage(String groupName, String memberName, String phoneNumber, String language);

    void sendPollStartMessage(String name, Date votingStart, Date votingEnd, String phoneNumber, String language);
}
