package com.eclectics.chamapoll.wrappers;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class VoteRequestWrapper {
    private long candidateId;
    private long positionId;
    private long pollId;
}
