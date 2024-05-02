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
public class TransactionLogsWrapper {
    private long id;
    private double updatedBalance;
    private double initialBalance;
    private double transactionAmount;
    private long creditAccountId;
    private String contributionNarration;
    private String debitPhonenUmber;
    private String capturedBy;
    private long contributionId;
    private String contributionsName;
    private long groupId;
    private String groupName;
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private Date createdOn;
}
