package com.eclectics.chamapayments.util;

import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@UtilityClass
public class RequestConstructor {

    public static Map<String, String> constructBody(String cbsAccount, String account, Integer amount, Long targetId, Optional<String> beneficiary, String transactionId, String scope) {
        Map<String, String> request = new HashMap<>();

        request.put("0", "0200");
        request.put("3", "400000");
        request.put("37", transactionId);
        request.put("65", "FUNDS_TRANSFER");
        // my resource
        request.put("61", String.valueOf(targetId));
        request.put("32", "VICOBA");

        switch (scope) {
            case "MC": // Member Contribution
                request.put("2", account);
                request.put("4", String.valueOf(amount));
                request.put("24", "MC");
                request.put("25", "MC");
                request.put("68", "Member contribution using WA");
                request.put("102", account);
                request.put("103", cbsAccount);
                break;
            case "MCC": // Member Contribution using Core Account
                request.put("2", account);
                request.put("4", String.valueOf(amount));
                request.put("24", "CC");
                request.put("25", "MCC");
                request.put("68", "Member contribution using CA");
                request.put("102", account);
                request.put("103", cbsAccount);
                break;
            case "MW": // Member Withdrawal to Wallet Account
                request.put("2", account);
                request.put("4", String.valueOf(amount));
                request.put("24", "CM");
                request.put("25", "MW");
                request.put("68", "Member withdrawal to WA");
                request.put("102", cbsAccount);
                request.put("103", account);
                break;
            case "MWC": // Member Withdrawal to Core Account
                request.put("2", account);
                request.put("4", String.valueOf(amount));
                request.put("24", "CC");
                request.put("25", "MW");
                request.put("68", "Member withdrawal to CA");
                request.put("102", cbsAccount);
                request.put("103", account);
                break;
            case "LD": // Loan Disbursement to WA
                request.put("2", account);
                request.put("4", String.valueOf(amount));
                request.put("24", "CM");
                request.put("25", "LD");
                request.put("68", "Loan disbursement to WA");
                request.put("102", cbsAccount);
                request.put("103", account);
                break;
            case "LDC": // Loan Disbursement to CA
                request.put("2", account);
                request.put("4", String.valueOf(amount));
                request.put("24", "CC");
                request.put("25", "LD");
                request.put("68", "Loan disbursement to CA");
                request.put("102", cbsAccount);
                request.put("103", account);
                break;
            case "LR": // Loan Repayment using WA
                request.put("2", account);
                request.put("4", String.valueOf(amount));
                request.put("24", "MC");
                request.put("25", "LR");
                request.put("68", "Loan repayment using WA");
                request.put("102", account);
                request.put("103", cbsAccount);
                break;
            case "LRC": // Loan Repayment using CA
                request.put("2", account);
                request.put("4", String.valueOf(amount));
                request.put("24", "CC");
                request.put("25", "LR");
                request.put("68", "Loan repayment using CA");
                request.put("102", account);
                request.put("103", cbsAccount);
                break;
            case "CO": // Contribution for another member using WA
                request.put("2", account);
                request.put("4", String.valueOf(amount));
                request.put("24", "MC");
                request.put("25", "CO");
                request.put("66", beneficiary.orElseThrow());
                request.put("68", "Other Contribution using WA");
                request.put("102", account);
                request.put("103", cbsAccount);
                break;
            case "COC": // Contribution for another member using CA
                request.put("2", account);
                request.put("4", String.valueOf(amount));
                request.put("24", "MCC");
                request.put("25", "COC");
                request.put("66", beneficiary.orElseThrow());
                request.put("68", "Other Contribution using CA");
                request.put("102", account);
                request.put("103", cbsAccount);
                break;
            default:
                break;
        }

        return request;
    }

    public static Map<String, String> getBalanceInquiryReq(String account) {
        Map<String, String> balanceInquiryReq = new HashMap<>();
        balanceInquiryReq.put("0", "0200");
        balanceInquiryReq.put("2", account);
        balanceInquiryReq.put("3", "310000");
        balanceInquiryReq.put("4", "0");
        balanceInquiryReq.put("24", "MM");
        balanceInquiryReq.put("32", "VICOBA");
        balanceInquiryReq.put("65", "BI");
        balanceInquiryReq.put("102", account);

        return balanceInquiryReq;
    }
}
