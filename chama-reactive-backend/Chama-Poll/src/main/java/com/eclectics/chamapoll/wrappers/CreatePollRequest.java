package com.eclectics.chamapoll.wrappers;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.lang.NonNull;

import java.util.Date;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class CreatePollRequest {
    @JsonProperty("description")
    private String description;
    @NonNull
    @JsonProperty("regStart")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date registrationStart;
    @NonNull
    @JsonProperty("regEnd")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date registrationEnd;
    @NonNull
    @JsonProperty("voteStart")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")

    private Date voteStart;
    @NonNull
    @JsonProperty("voteEnd")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date voteEnd;
    @NonNull
    @JsonProperty("groupId")
    private Long groupId;
}
