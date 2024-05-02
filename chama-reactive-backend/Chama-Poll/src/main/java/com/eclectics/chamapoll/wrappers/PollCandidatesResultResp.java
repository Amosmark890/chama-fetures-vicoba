package com.eclectics.chamapoll.wrappers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class PollCandidatesResultResp {
    private long candidateId;
    private String firstName;
    private String lastName;
    private int totalVotes;

}
