package com.eclectics.chamapayments.wrappers.request;

import lombok.*;

/**
 * @author Alex Maina
 * @created 21/12/2021
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class LoanRepaymentsWrapper {
    private Long loandisbursedid;
    private Double amount;
    private String receiptnumber;
    private String paymentPhoneNumber;
    private Long groupAccountId;
    private String coreAccount = "";
    private boolean approve = true;
    private Long loanpaymentid;
    private Long groupId;
}
