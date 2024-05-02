package com.eclectics.chamapayments.wrappers.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class AccountWrapper {
    @NotEmpty(message = "account name cannot be empty") @NotNull(message = "account name cannot be empty")
    String accountname;
    /**
     * The Accounttypeid.
     */
    @NotNull(message = "account type cannot be empty")
    long accounttypeid;
    /**
     * The Accountnumber.
     */
    /**
     * The Groupid.
     */
    @NotNull(message = "groupid cannot be empty")
    long groupid;
    @NotNull(message = "account balance cannot be empty")
    double accountbalance;

    @ApiModelProperty(required = true,
            value = "this is a json object containing the details according to the various account" +
                    " types, e.g. if there's bank branch details, accountnumber, investment description")
    private Map<String, Object> accountdetails;

//    @NotEmpty(message = "account number cannot be empty") @NotNull(message = "account number cannot be empty")
//    @ApiModelProperty(value = "")
//    String accountnumber;
//    @ApiModelProperty(value = "")
//    String description;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    long accountid;

}
