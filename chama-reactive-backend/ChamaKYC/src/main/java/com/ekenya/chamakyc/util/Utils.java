package com.ekenya.chamakyc.util;

import lombok.experimental.UtilityClass;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class Utils {

    public static String generate4DigitsOTP(){
        SecureRandom r = new SecureRandom();
        int low = 1000;
        int high = 9999;
        int result = r.nextInt(high-low) + low;
        return String.valueOf(result);

    }

    public static String generate6DigitsOtp(){
        SecureRandom r = new SecureRandom();
        int low = 100000;
        int high = 999999;
        int result = r.nextInt(high-low) + low;
        return String.valueOf(result);

    }

    public static Map<String, String> mapToRequestBody(String scope, String... args) {
        Map<String, String> requestBody = new HashMap<>();

        String account = args[0];

        switch (scope) {
            case "groupLookup":
            case "userLookup":
                requestBody.put("0", "0200");
                requestBody.put("2", account);
                requestBody.put("3", "990000");
                requestBody.put("4", "0");
                requestBody.put("24", "MM");
                requestBody.put("32", "VICOBA");
                requestBody.put("65", "ACCOUNT_LOOKUP");
                requestBody.put("102", account);
                break;
            default:
                break;
        }

        return requestBody;
    }

}
