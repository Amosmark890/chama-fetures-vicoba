package com.eclectics.chamapayments.wrappers.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.util.Date;

/**
 * @author Alex Maina
 * @created 06/12/2021
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentApproval implements Comparable<PaymentApproval> {
    private long paymentid;
    private long creditaccountid;
    private String creditaccountname;
    private String creditaccounttype;
    private String debitaccount;
    private String narration;
    private double amount;
    private long contributionid;
    private String capturedby;
    private String receiptImageUrl;
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private Date appliedon;

    @Override
    public int compareTo(PaymentApproval paymentApproval) {
        return appliedon.compareTo(paymentApproval.appliedon);
    }
}
