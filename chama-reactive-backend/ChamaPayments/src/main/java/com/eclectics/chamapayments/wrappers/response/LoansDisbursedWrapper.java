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
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoansDisbursedWrapper implements Comparable<LoansDisbursedWrapper> {
    private long accountTypeId;
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private Date appliedOn;
    private long contributionId;
    private String  contributionName;
    private double dueAmount;
    private double principle;
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private Date dueDate;
    private long groupId;
    private String groupName;
    private double interest;
    private String isGuarantor;
    private String recipient;
    private String recipientNumber;
    private long loanId;
    private String loanName;
    private String approvedBy;
    private double principal;

    @Override
    public int compareTo(LoansDisbursedWrapper disbursed) {
        return appliedOn.compareTo(disbursed.appliedOn);
    }
}
