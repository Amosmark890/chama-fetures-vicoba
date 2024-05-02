package com.eclectics.chamapoll.wrappers;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.lang.NonNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VyePositionRequest {
    @NonNull
    private long positionId;
    @NonNull
    private long  pollId;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String phoneNumber;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private long groupId;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String positionName;
}
