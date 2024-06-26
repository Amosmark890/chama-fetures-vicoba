package com.ekenya.chamakyc.wrappers.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.Map;


@Getter
@Setter
public class UpdateMemberPermissionsRequest {
    @NotNull(message = "groupid cannot be null")
    long groupid;
    @NotNull(message = "phonenumber cannot be null")
    String phonenumber;
    @NotNull(message = "permissions cannot be null")
    Map<String, Object> permissions;
}
