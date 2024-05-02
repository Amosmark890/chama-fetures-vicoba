package com.eclectics.notifications.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

/**
 * Configure the SMS Engine.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VicobaTextEngine {

    @Value("${vicoba.sms}")
    public String smsUrl;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        webClient = WebClient.builder().baseUrl(smsUrl).build();
    }

    /**
     * Send text.
     *
     * @param text        the message
     * @param phoneNumber the phone number
     */
    public void sendText(String text, String phoneNumber) {
        log.info("Sending DCB text... To: {} Message: {}", phoneNumber, text);

        Mono<String> response = webClient
                .get()
                .uri(uriBuilder -> uriBuilder.path("/SMSHTTPServerV1.1/HTTPServlet")
                        .queryParam("id", System.currentTimeMillis())
                        .queryParam("phoneNumber", phoneNumber)
                        .queryParam("message", text)
                        .queryParam("channel", "POS")
                        .build()
                )
                .retrieve()
                .bodyToMono(String.class);

        response.subscribe(s -> log.info("Response from DCB SMS Gateway... {}", s));
    }
}
