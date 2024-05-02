package com.ekenya.chamakyc.wrappers.response;

import com.ekenya.chamakyc.wrappers.request.GroupMembershipWrapper;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PinValidationMetadata {
    String linkedAccounts;
    List<GroupInvitesWrapper> invites;
    List<GroupMembershipWrapper> groupMembership;
}
