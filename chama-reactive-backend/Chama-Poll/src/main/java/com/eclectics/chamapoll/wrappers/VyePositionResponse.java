package com.eclectics.chamapoll.wrappers;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class VyePositionResponse {
    private String status;
    private String firstName;
    private String lastName;
    private long positionId;
    private long candidateId;
    private String positionName;
}
