package com.eclectics.chamapoll.wrappers;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PollResultRequest {
    private Long pollId;
    private Long groupId;
}
