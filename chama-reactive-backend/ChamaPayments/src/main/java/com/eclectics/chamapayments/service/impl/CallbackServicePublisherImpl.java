package com.eclectics.chamapayments.service.impl;

import com.eclectics.chamapayments.service.CallbackServicePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static com.eclectics.chamapayments.service.constants.KafkaChannelsConstants.CALLBACK_TOPIC;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallbackServicePublisherImpl implements CallbackServicePublisher {

    private final StreamBridge streamBridge;

    @Override
    public void publishCallback(String body) {
        Mono.fromRunnable(() -> {
            log.info("Publishing Callback::: {}", body);
            streamBridge.send(CALLBACK_TOPIC, body);
        }).subscribeOn(Schedulers.boundedElastic()).publishOn(Schedulers.boundedElastic()).subscribe();
    }
}
