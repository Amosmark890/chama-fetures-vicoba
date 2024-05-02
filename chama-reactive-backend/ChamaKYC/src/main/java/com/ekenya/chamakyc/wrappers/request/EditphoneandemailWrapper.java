package com.ekenya.chamakyc.wrappers.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * Class name: EditphoneandemailWrapper
 * Creater: wgicheru
 * Date:3/30/2020
 */
@Getter
@Setter
public class EditphoneandemailWrapper {
    @NotNull(message = "oldphone cannot be null") @NotEmpty(message = "oldphone cannot be empty")
    String oldphone;
    @NotNull(message = "newphone cannot be null") @NotEmpty(message = "newphone cannot be empty")
    String newphone;
    @NotNull(message = "newemail cannot be null") @NotEmpty(message = "newemail cannot be empty")
    String newemail;
}
