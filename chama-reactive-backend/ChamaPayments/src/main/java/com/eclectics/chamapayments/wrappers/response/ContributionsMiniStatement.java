package com.eclectics.chamapayments.wrappers.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class ContributionsMiniStatement {
    private String date;
    private String memberName;
    private String amount;
}
