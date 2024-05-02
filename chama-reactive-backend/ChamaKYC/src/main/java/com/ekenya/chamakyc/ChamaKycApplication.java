package com.ekenya.chamakyc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.ekenya.chamakyc"})
@EnableScheduling
@EnableAsync
public class ChamaKycApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChamaKycApplication.class, args);
    }

}
