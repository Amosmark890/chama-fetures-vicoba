package com.ekenya.chamakyc.wrappers.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupInvitesWrapper {
    private long id;
    private String status;
    private long groupId;
    private String groupName;
}
