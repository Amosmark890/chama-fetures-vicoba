package com.ekenya.chamaauthorizationserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;

@SpringBootApplication
@EnableDiscoveryClient
@EnableAuthorizationServer
public class ChamaAuthorizationServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChamaAuthorizationServerApplication.class, args);
    }
}
