package com.eclectics.chamapayments.wrappers.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class PayPenaltyLoanWrapper {
    @NotNull(message = "penalty id cannot be  be null")
    @NotEmpty(message = "penalty id cannot cannot be empty")
    private Long penaltyLoanId;

    @NotNull(message = "Amount cannot be  be null")
    @NotEmpty(message = "Amount cannot cannot be empty")
    private Double amount;
    private String paymentPhoneNumber;
    private String receiptNumber;
}
