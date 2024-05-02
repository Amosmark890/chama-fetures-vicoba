package com.eclectics.commons.wrappers.Request;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * Class name: ApproveLoanWrapper
 * Creater: wgicheru
 * Date:4/22/2020
 */
@Data
public class ApproveLoanWrapper {
    @NotNull(message = "approve cannot be null")
    boolean approve;
    @NotNull(message = "loanapplicationid cannot be null")
    long loanapplicationid;
//    @NotNull(message = "accountid cannot be null")
    long accountid;
}
