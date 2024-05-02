package com.eclectics.chamapayments.wrappers.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * Class name: MakecontributionWrapper
 * Creater: wgicheru
 * Date:3/18/2020
 */
@Getter
@Setter
public class MakecontributionWrapper {
    /**
     * The Creditaccountid.
     */
    @NotNull(message = "creditaccountid cannot be empty")
    @ApiModelProperty(value = "the account to which the funds are/were loaded to")
    long creditaccountid;
    /**
     * The Debitaccount.
     */
    @NotNull(message = "debitaccount cannot be null") @NotEmpty(message = "debitaccount cannot be empty")
    @ApiModelProperty(value = "the phonenumber of  the member making the contribution")
    String debitaccount;
    /**
     * The Amount.
     */
    @NotNull(message = "amount cannot be empty")
    double amount;
    /**
     * The Contributionid.
     */
    long contributionid;

    String receiptNumber;
    Long groupaccountid;
    @NotNull(message = " isPenaltyPayment cannot be empty")
    Boolean isPenaltyPayment;
    Long penaltyId;
    @NotNull(message = "schedule payment id cannot be empty")
    String schedulePaymentId;
    @NotNull(message = "isCombinedPayment payment id cannot be empty")
    Boolean isCombinedPayment;
}
