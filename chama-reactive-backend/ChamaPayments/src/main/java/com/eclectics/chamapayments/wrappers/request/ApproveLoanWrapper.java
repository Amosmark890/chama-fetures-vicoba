package com.eclectics.chamapayments.wrappers.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class ApproveLoanWrapper {
    @NotNull(message = "approve cannot be null")
    boolean approve;
    @NotNull(message = "loanapplicationid cannot be null")
    long loanapplicationid;
    long accountid;
    long groupid;
}
