package com.eclectics.chamapayments.wrappers.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpcomingContributionsWrapper {
    private String contributionName;
    private String schedulePaymentId;
    private Integer amount;
    private Long groupId;
    private Boolean hasPenalty;
    private Long penaltyId;
    private Integer penaltyAmount;
    private Integer remaining;
    private String expectedPaymentDate;
    private Integer outstandingAmount = 0;
}
