package com.eclectics.chamapayments.wrappers.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface LoanRepaymentsProjection {
    @JsonProperty(value = "transactionId")
    String getTransactionId();
    @JsonProperty(value = "loanee")
    String getLoanee();
    @JsonProperty(value = "phoneNumber")
    String getPhonenumber();
    @JsonProperty(value = "principal")
    Double getPrincipal();
    @JsonProperty(value = "amount")
    Double getAmount();
    @JsonProperty(value = "balance")
    Double getBalance();
    @JsonProperty(value = "status")
    String getStatus();
}
