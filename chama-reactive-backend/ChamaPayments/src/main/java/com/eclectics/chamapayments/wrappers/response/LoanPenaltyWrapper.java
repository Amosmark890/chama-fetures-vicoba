package com.eclectics.chamapayments.wrappers.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoanPenaltyWrapper {
    private Long loanPenaltyId;
    private Double penaltyAmount;
    private String paymentStatus;
    private Double paidAmount;
    private Double dueAmount;
    private String transactionId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private Date loanDueDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private Date lastPaymentDate;
    private String memberName;
    private String memberPhoneNumber;
}
