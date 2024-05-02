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
public class WithdrawalApproval implements Comparable<WithdrawalApproval> {
    private String debitaccountname;
    private long debitaccountid;
    private String debitaccounttype;
    private String creditaccount;
    private double amount;
    private long contributionid;
    private String capturedby;
    private String withdrawal_narration;
    private String withdrawalreason;
    private String status;
    private long requestid;
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private Date appliedon;

    @Override
    public int compareTo(WithdrawalApproval approval) {
        return appliedon.compareTo(approval.appliedon);
    }
}
