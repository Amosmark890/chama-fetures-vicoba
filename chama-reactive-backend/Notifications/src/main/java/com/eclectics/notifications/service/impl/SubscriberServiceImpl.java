package com.eclectics.notifications.service.impl;

import com.eclectics.notifications.service.SendMailEngine;
import com.eclectics.notifications.service.SubscriberService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriberServiceImpl implements SubscriberService {

    private final SendTextEngine sendTextEngine;
    private final VicobaTextEngine vicobaTextEngine;
    private final SendMailEngine sendMailEngine;

    @Bean
    @Override
    public Consumer<String> sendText() {
        return textInfo -> {
            JsonObject jsonObject = new JsonParser().parse(textInfo).getAsJsonObject();

            sendTextEngine.sendText(jsonObject.get("message").getAsString(), jsonObject.get("phoneNumber").getAsString());
        };
    }

    @Bean
    @Override
    public Consumer<String> sendVicobaText() {
        return textInfo -> {
            JsonObject jsonObject = new JsonParser().parse(textInfo).getAsJsonObject();

            vicobaTextEngine.sendText(jsonObject.get("message").getAsString(), jsonObject.get("phoneNumber").getAsString());
        };
    }

    @Bean
    @Override
    public Consumer<String> sendEmail() {
        return emailInfo -> {
            log.info("Email info... {}", emailInfo);
            JsonObject jsonObject = new JsonParser().parse(emailInfo).getAsJsonObject();

            String title = jsonObject.get("title").getAsString();
            String message = jsonObject.get("message").getAsString();
            String email = jsonObject.get("email").getAsString();
            sendMailEngine.sendMail(title, message, email);
        };
    }
}
