package com.eclectics.chamapayments.util;

import com.eclectics.chamapayments.model.EsbRequestLog;
import com.eclectics.chamapayments.repository.EsbRequestLogRepository;
import com.google.gson.JsonObject;
import lombok.experimental.UtilityClass;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.SecureRandom;
import java.util.Map;

@UtilityClass
public class TransactionIdGenerator {
//    @Autowired
//    EsbRequestLogRepository esbRequestLogRepository;

    public static String generateTransactionId(String prefix) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        sb.append(saltRandom(generateSevenDigit()));
        return sb.toString();
    }

    private static String saltRandom(String randomNumber){
        char[] chars = new char[] {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < randomNumber.length(); i++) {
            if (i % 2 == 0) {
                sb.append(randomNumber.charAt(i));
                continue;
            }

            sb.append(chars[generateOneDigit()]);
        }

        return sb.toString();
    }

    private static int generateOneDigit() {
        SecureRandom r = new SecureRandom();
        int low = 0;
        int high = 25;
        return r.nextInt(high-low) + low;
    }

    private static String generateSevenDigit(){
        SecureRandom r = new SecureRandom();
        int low = 1000000;
        int high = 9999999;
        int result = r.nextInt(high-low) + low;
        return String.valueOf(result);
    }

//    public static EsbRequestLog saveEsbRequestLog(Map<String, String> body) {
//        EsbRequestLog esbRequestLog = EsbRequestLog.builder()
//                .field0(body.getOrDefault("0", null))
//                .field2(body.getOrDefault("2", null))
//                .field3(body.getOrDefault("3", null))
//                .field4(body.getOrDefault("4", null))
//                .field24(body.getOrDefault("24", null))
//                .field25(body.getOrDefault("25", null))
//                .field32(body.getOrDefault("32", null))
//                .field37(body.getOrDefault("37", null))
//                .field61(body.getOrDefault("61", null))
//                .field65(body.getOrDefault("65", null))
//                .field66(body.getOrDefault("66", null))
//                .field68(body.getOrDefault("68", null))
//                .field102(body.getOrDefault("102", null))
//                .field103(body.getOrDefault("103", null))
//                .build();
//
//        return esbRequestLogRepository.save(esbRequestLog);
//    }


}
