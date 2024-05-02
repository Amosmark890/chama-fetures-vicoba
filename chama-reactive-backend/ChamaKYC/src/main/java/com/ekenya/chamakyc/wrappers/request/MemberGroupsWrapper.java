package com.ekenya.chamakyc.wrappers.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
public class MemberGroupsWrapper {
    private Optional<Long> groupId = Optional.empty();
    private Optional<Integer> page = Optional.empty();
    private Optional<Integer> size = Optional.empty();
}
