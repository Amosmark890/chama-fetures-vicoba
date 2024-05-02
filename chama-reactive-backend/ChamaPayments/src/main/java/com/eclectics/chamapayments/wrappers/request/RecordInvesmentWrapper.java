package com.eclectics.chamapayments.wrappers.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * Class name: RecordInvesmentWrapper
 * Creater: wgicheru
 * Date:4/6/2020
 */
@Getter
@Setter
public class RecordInvesmentWrapper {
    @NotNull(message = "name cannot be null") @NotEmpty(message = "name cannot be empty")
    String investmentname;
    @NotNull(message = "groupid cannot be null")
    long groupid;
    @ApiModelProperty(value = "the estimated amount the investment is valued at")
    @NotNull(message = "investmentvalue cannot be null")
    double investmentvalue;
    @ApiModelProperty(value = "the details for other members to understand the investment better")
    @NotNull(message = "description cannot be null") @NotEmpty(message = "description cannot be empty")
    String description;
    @ApiModelProperty(value = "the member phonenumber of the one managing the investment")
    @NotNull(message = "managedby cannot be null") @NotEmpty(message = "managedby cannot be empty")
    String managedby;
    @ApiModelProperty(value = "use this field when updating a record that has already been createdOn")
    long investmentid;
}
