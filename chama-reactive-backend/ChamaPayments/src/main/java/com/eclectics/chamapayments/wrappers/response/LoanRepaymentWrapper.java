package com.eclectics.chamapayments.wrappers.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoanRepaymentWrapper {
    private long id;
    private double memberId;
    private String memberName;
    private double initialLoan;
    private double paidAmount;
    private double balance;
    private long groupId;
    private String groupName;
    private String paymentType;
    private String receiptNumber;
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private Date createdDate;
}
