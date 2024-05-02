package com.ekenya.chamakyc.wrappers.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MembersInGroupRequestWrapper {
    private long groupId;
    private int page;
    private int size;
}
