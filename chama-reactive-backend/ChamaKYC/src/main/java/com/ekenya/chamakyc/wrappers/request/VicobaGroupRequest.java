package com.ekenya.chamakyc.wrappers.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class VicobaGroupRequest {
    private String actualBalance;
    private String accountType;
    private String accountName;
    private String isActive;
    private boolean exists;
    private String availableBalance;
    private String groupName;
    private long groupId;
    private boolean active;
}
