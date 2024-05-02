package com.ekenya.chamakyc.wrappers.request;

import lombok.*;

import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupInvitesRequestWrapper {
    private Optional<Long> groupId;
    private Integer page;
    private Integer size;
}
