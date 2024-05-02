package com.eclectics.chamapayments.wrappers.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface LoanApplicationsProjection {
    @JsonProperty(value = "recipientName")
    String getRecipientname();
    @JsonProperty(value = "phoneNumber")
    String getPhonenumber();
    @JsonProperty(value = "principal")
    Double getPrincipal();
    @JsonProperty(value = "amount")
    Double getAmount();
    @JsonProperty(value = "dueAmount")
    Double getDueamount();
    @JsonProperty(value = "interest")
    Double getInterest();
    @JsonProperty(value = "status")
    String getStatus();
}
