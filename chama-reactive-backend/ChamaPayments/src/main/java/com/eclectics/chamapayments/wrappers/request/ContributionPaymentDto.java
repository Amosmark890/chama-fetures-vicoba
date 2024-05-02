package com.eclectics.chamapayments.wrappers.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
public class ContributionPaymentDto {
    private Long groupId;
    private Integer amount;
    private String schedulePaymentId;
    private String beneficiary;
    @Size(max = 15, message = "Length cannot be more than 15")
    private String coreAccount = "";
    private Boolean isPenaltyPayment;
    private Long penaltyId;
}
