package com.eclectics.chamapayments.service.impl;

import com.eclectics.chamapayments.service.PublishingService;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import static com.eclectics.chamapayments.service.constants.KafkaChannelsConstants.SEND_EMAIL_TOPIC;
import static com.eclectics.chamapayments.service.constants.KafkaChannelsConstants.SEND_TEXT_TOPIC;

/**
 * @author Alex Maina
 * @created 07/12/2021
 */
@Service
@RequiredArgsConstructor
public class PublishingServiceImpl implements PublishingService {

    private final StreamBridge streamBridge;

    @Override
    public void sendEmailNotification(String title, String message, String email) {
        JsonObject emailObject = new JsonObject();
        emailObject.addProperty("title", title);
        emailObject.addProperty("message", message);
        emailObject.addProperty("email", email);

        streamBridge.send(SEND_EMAIL_TOPIC, emailObject.toString());
    }

    @Override
    public void sendText(String message, String phoneNumber) {
        JsonObject textObject = new JsonObject();
        textObject.addProperty("message", message);
        textObject.addProperty("phoneNumber", phoneNumber);

        streamBridge.send(SEND_TEXT_TOPIC, textObject.toString());
    }
}
