package com.eclectics.chamapayments.model.jpaInterfaces;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface UpcomingContributionsProjection {
    @JsonProperty(value = "contributionName")
    String getContributioname();
    @JsonProperty(value = "schedulePaymentId")
    String getSchedulepaymentid();
    @JsonProperty(value = "amount")
    int getAmount();
    @JsonProperty(value = "groupId")
    long getGroupid();
    @JsonProperty(value = "remaining")
    int getRemainder();
    @JsonProperty(value = "expectedPaymentDate")
    String getExpectedpaymentdate();
}
