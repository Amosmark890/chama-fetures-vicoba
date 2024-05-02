package com.eclectics.notifications.service.impl;

import com.eclectics.notifications.model.SmsRequestBody;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Configure the SMS Engine.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SendTextEngine {

    @Value("${app-configs.sms-url}")
    public String smsUrl;
    @Value("${app-configs.pg-client-username}")
    public String pgUsername;
    @Value("${app-configs.pg-client-id}")
    public String pgClientId;
    @Value("${app-configs.pg-client-name}")
    public String pgClientName;
    @Value("${app-configs.pg-client-password}")
    public String pgClientPassword;

    private final WebClient webClient;

    /**
     * Send text.
     *
     * @param text        the message
     * @param phoneNumber the phone number
     */
    public void sendText(String text, String phoneNumber) {
        SmsRequestBody smsRequestBody = new SmsRequestBody();
        smsRequestBody.setTo(phoneNumber);
        smsRequestBody.setMessage(text);
        smsRequestBody.setFrom(pgClientName);
        smsRequestBody.setTransactionID(UUID.randomUUID().toString());
        smsRequestBody.setClientid(pgClientId);
        smsRequestBody.setPassword(pgClientPassword);
        smsRequestBody.setUsername(pgUsername);

        Gson gson = new Gson();
        log.info("===SENDING SMS {}", gson.toJson(smsRequestBody));

        Mono<String> response = webClient
                .post()
                .uri(smsUrl)
                .bodyValue(gson.toJson(smsRequestBody))
                .retrieve()
                .bodyToMono(String.class);

        response.subscribe(s -> log.info("Response from SMS Gateway... {}", s));
    }
}
