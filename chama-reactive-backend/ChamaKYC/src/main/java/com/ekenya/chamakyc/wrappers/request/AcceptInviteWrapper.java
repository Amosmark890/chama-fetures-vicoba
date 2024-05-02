package com.ekenya.chamakyc.wrappers.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AcceptInviteWrapper {
    private long inviteId;
    private String action;
}
