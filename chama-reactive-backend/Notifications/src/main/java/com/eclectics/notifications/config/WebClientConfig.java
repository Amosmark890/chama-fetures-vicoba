package com.eclectics.notifications.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Provide WebcClient using DI.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient provideWebClient() {
        return WebClient.builder().build();
    }

}
