package com.ekenya.chamakyc.wrappers.request;

import lombok.*;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * The type Verify otp wrapper.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpWrapper {
    @NotNull(message = "phone number cannot be null") @NotEmpty(message = "phone number cannot be empty")
    private String phoneNumber;
    @NotNull(message = "otp cannot be null")
    private int otp;
}
