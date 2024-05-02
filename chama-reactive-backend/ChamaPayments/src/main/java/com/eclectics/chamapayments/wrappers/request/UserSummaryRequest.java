package com.eclectics.chamapayments.wrappers.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserSummaryRequest {
    private String phone;
    private String contributionId;
}
