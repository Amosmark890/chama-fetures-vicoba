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
@Builder
@Getter
@Setter
public class WithdrawLogsWrapper {
    private String transactionId;
    private double amount;
    private String contributionNarration;
    private long debitAccountId;
    private long contributionAccount;
    private boolean isContribution;
    private String contributionName;
    private boolean isLoanApplication;
    private long loanApplicationId;
    private String creditUserNumber;
    private double newBalance;
    private double oldBalance;
    private String capturedBy;
    private String reason;
    private String transferToUserStatus;
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private Date createdOn;
}
