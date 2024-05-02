package com.ekenya.chamakyc.wrappers.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * The type Password updater.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordUpdater {
    //    @NotNull(message = "phonenumber cannot be null") @NotEmpty(message = "phonenumber cannot be empty")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String phonenumber;
    @NotNull(message = "password cannot be null")
    @NotEmpty(message = "password cannot be empty")
    private String password;
    @NotNull(message = "otp cannot be null")
    @NotEmpty(message = "otp cannot be empty")
    private String otp;
}
