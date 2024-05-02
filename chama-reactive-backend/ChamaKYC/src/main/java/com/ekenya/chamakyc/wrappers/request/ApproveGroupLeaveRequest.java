package com.ekenya.chamakyc.wrappers.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApproveGroupLeaveRequest {
    private String phonenumber;
    private Long groupid;
    private Boolean approve;
}
