package com.eclectics.chamapoll.wrappers;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PollPositionRequest {
    private Long pollPosition;
    private Long pollId;
}
