package com.eclectics.chamapayments.wrappers.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.util.Date;

/**
 * @author Alex Maina
 * @created 08/12/2021
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class LoanPenaltyReportWrapper {
    private Long loanPenaltyId;
    private Double penaltyAmount;
    private String paymentStatus;
    private Double paidAmount;
    private Double dueAmount;
    private String transactionId;
    private String loanDueDate;
    private Date lastPaymentDate;
    private String groupName;
    private String memberName;
    private String memberPhoneNumber;
    private double loanDisbursedAmount;
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private Date createdOn;
}
