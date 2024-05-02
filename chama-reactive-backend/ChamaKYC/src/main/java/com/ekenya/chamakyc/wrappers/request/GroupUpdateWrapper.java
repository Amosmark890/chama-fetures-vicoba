package com.ekenya.chamakyc.wrappers.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GroupUpdateWrapper {
    private long groupId;
    private String name;
}
