package com.eclectics.chamapoll.wrappers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;

@AllArgsConstructor
@Getter
@Setter
public class ExitPollRequest {
    @NonNull
    private Long positionId;
    @NonNull
    private long  pollId;
}
