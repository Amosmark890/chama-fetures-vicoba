package com.eclectics.chamapoll.wrappers;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelPollWrapper {
    private Long pollId;
    private Long groupId;
}
