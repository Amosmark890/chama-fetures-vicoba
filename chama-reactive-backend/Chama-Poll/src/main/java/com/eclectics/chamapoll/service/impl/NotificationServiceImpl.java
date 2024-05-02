package com.eclectics.chamapoll.service.impl;

import com.eclectics.chamapoll.model.MessageTemplates;
import com.eclectics.chamapoll.repository.MessageTemplatesRepo;
import com.eclectics.chamapoll.service.NotificationService;
import com.eclectics.chamapoll.service.PublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final PublishService publishService;
    private final MessageTemplatesRepo messageTemplatesRepo;

    @Override
    public void sendDeleteGroupMessage(String groupName, String memberName, String phoneNumber, String language) {
        MessageTemplates messageTemplate = messageTemplatesRepo.findByTypeAndLanguage("delete_poll", language);
        String message = String.format(messageTemplate.getTemplate(), groupName, memberName);

        publishService.sendText(message, phoneNumber);
    }

    @Override
    public void sendPollStartMessage(String groupName, Date votingStart, Date votingEnd, String phoneNumber, String language) {
        MessageTemplates messageTemplate = messageTemplatesRepo.findByTypeAndLanguage("poll_started", language);
        String message = String.format(messageTemplate.getTemplate(), groupName, votingStart, votingEnd);

        publishService.sendText(message, phoneNumber);
    }
}
