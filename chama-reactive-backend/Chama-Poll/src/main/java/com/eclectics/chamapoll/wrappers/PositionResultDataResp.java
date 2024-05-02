package com.eclectics.chamapoll.wrappers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class PositionResultDataResp {
    private Long positionId;
    private String positionName;
    private int noOfCandidates;
    private int totalVotesCasted;
    private List<PollCandidatesResultResp> pollCandidatesResultRespList;
}
