package com.eclectics.commons.wrappers.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * The type Verify otp wrapper.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpWrapper {
    @NotNull(message = "phonenumber cannot be null") @NotEmpty(message = "phonenumber cannot be empty")
    private String phonenumber;
    @NotNull(message = "otp cannot be null")
    private int otp;
}
