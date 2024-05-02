package com.eclectics.chamapayments.wrappers.response;

import lombok.Getter;
import lombok.Setter;

/**
 * Class name: PaymentApprovalWrapper
 * Creater: wgicheru
 * Date:4/16/2020
 */
@Getter
@Setter
public class PaymentApprovalWrapper {
    long paymentid;
    long creditaccountid;
    String creditaccountname;
    String creditaccounttype;
    String debitaccount;
    String narration;
    double amount;
    long contributionid;
    String capturedby;
    String receiptImageUrl;
    String appliedon;
}
