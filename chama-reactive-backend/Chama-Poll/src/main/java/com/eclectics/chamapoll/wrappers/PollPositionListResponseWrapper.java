package com.eclectics.chamapoll.wrappers;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PollPositionListResponseWrapper {
    private long id;
    private String name;
    private int candidates;
    private boolean vyed;
}
