package com.eclectics.chamapoll.wrappers;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class PollVoteDataResp {
    private Long pollId;
    private String pollName;
    private String pollDescription;
    private Long totalGroupMembers;
    private String status;
    // This returns if the Poll is accepting any resgistration
    private Boolean isVying;
    // This is the id that indicates the user is a candidate in any of the polls in the group
    private Long vyingPositionId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private Date registrationStart;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private Date registrationEnd;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private Date voteStart;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private Date voteEnd;
    private List<PositionResultDataResp> positionData;

}
