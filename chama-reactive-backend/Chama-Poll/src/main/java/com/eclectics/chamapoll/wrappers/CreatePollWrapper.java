package com.eclectics.chamapoll.wrappers;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.NonNull;

import java.util.Date;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class CreatePollWrapper {
    @JsonProperty("description")
    private String description;
    @NonNull
    @JsonProperty("regStart")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private Date registrationStart;
    @NonNull
    @JsonProperty("regEnd")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private Date registrationEnd;
    @NonNull
    @JsonProperty("voteStart")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private Date voteStart;
    @NonNull
    @JsonProperty("voteEnd")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private Date voteEnd;
    @NonNull
    @JsonProperty("groupId")
    private Long groupId;
    @NonNull
    @JsonProperty("positions")
    private PollPositionWrapper positions;
    @JsonProperty("candidates")
    private PollCandidatesWrapper pollCandidatesWrapper;
}
