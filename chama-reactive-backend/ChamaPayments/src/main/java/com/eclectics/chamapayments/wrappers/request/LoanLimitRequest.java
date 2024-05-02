package com.eclectics.chamapayments.wrappers.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoanLimitRequest {
    private Long contributionId;
    private Long groupId;
    private Long loanProductId;
}
