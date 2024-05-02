package com.eclectics.chamapayments.wrappers.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.util.Date;

/**
 * @author Alex Maina
 * @created 08/12/2021
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContributionPaymentWrapper {
    private long contributionPaymentId;
    private String contributionName;
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private Date contributionStartDate;
    private String groupName;
    private String contributionType;
    private String scheduleType;
    private String transactionId;
    private String paymentStatus;
    private double amount;
    private String phoneNumber;
    private Date createdOn;
    private String mpesaPaymentId;
    private String paymentFailureReason;
    private String mpesaCheckoutId;
    private String paymentType;
    private String groupAccountId;
    private String receiptImageUrl;
    private boolean isPenalty;
    private String schedulePaymentId;
    private boolean isCombinedPayment;
}
