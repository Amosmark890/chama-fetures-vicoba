package com.eclectics.chamapayments.wrappers.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupContributionsRequest {
    private Long contributionId;
    private Integer page=0;
    private Integer size;
}
