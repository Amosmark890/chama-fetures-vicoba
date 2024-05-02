package com.eclectics.chamapayments;

import com.google.gson.Gson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.NumberFormat;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class ChamaPaymentsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChamaPaymentsApplication.class, args);
    }

    @Bean
    public Gson gson(){
        return new Gson();
    }

    @Bean
    public NumberFormat numberFormat() {
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setGroupingUsed(true);
        return numberFormat;
    }

    @Bean
    @LoadBalanced
    public WebClient loadBalancedWebClientBuilder() {
        return WebClient.builder().build();
    }
}
