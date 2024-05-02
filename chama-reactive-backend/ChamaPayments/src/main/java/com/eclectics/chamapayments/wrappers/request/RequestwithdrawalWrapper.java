package com.eclectics.chamapayments.wrappers.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * Class name: RequestwithdrawalWrapper
 * Creater: wgicheru
 * Date:4/3/2020
 */
@Getter
@Setter
public class RequestwithdrawalWrapper {

    @NotNull(message = "account cannot be empty")
    @ApiModelProperty(value = "the account in which the funds are held")
    long debitaccountid;

    @NotNull(message = "debitaccount cannot be null") @NotEmpty(message = "debitaccount cannot be empty")
    @ApiModelProperty(value = "the phonenumber of the member making the withdrawal, funds will be given to this member")
    String creditaccount;
    /**
     * The Amount.
     */
    @NotNull(message = "amount cannot be empty")
    double amount;
    /**
     * The Contributionid.
     */
    @NotNull(message = "contributionid cannot be empty")
    long contributionid;

    @NotNull(message = "withdrawalreason cannot be empty")
    String withdrawalreason;

    String coreAccount;
}
