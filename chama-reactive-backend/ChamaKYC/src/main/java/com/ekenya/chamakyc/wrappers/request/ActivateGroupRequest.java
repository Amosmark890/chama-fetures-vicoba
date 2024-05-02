package com.ekenya.chamakyc.wrappers.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import static com.ekenya.chamakyc.util.StringConstantsUtil.CBS_ACCOUNT_MATCH;

@Getter
@Setter
public class ActivateGroupRequest {
    @NotNull(message = "Group id cannot be null")
    @NotEmpty(message = "Group id cannot be empty")
    private Long groupId;
    @Size(max = 15, message = "Account number can only have a max of 15 digits")
    @Pattern(regexp = CBS_ACCOUNT_MATCH, message = "Account number cannot contain letters or special characters")
    private String cbsAccount = "";
}
