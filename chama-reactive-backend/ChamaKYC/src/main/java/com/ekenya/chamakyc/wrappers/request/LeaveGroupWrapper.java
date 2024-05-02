package com.ekenya.chamakyc.wrappers.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class LeaveGroupWrapper {
    @NotNull(message = "groupid cannot be empty")
    long groupid;
    String reason;
    String memberphonenumber;
}
