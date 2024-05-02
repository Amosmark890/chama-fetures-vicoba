package com.eclectics.chamapayments.wrappers.response;

import lombok.Getter;
import lombok.Setter;

/**
 * Class name: WithdrawalApprovalWrapper
 * Creater: wgicheru
 * Date:4/17/2020
 */
@Getter
@Setter
public class WithdrawalApprovalWrapper {
    String debitaccountname;
    long debitaccountid;
    String debitaccounttype;
    String creditaccount;
    double amount;
    long contributionid;
    String capturedby;
    String withdrawal_narration;
    String withdrawalreason;
    long requestid;
    private String appliedon;
}
