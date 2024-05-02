package com.eclectics.chamapoll.wrappers;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PollCandidatesWrapper {
    private List<String> chairman = Collections.emptyList();
    private List<String> secretary = Collections.emptyList();
    private List<String> treasurer = Collections.emptyList();
}
