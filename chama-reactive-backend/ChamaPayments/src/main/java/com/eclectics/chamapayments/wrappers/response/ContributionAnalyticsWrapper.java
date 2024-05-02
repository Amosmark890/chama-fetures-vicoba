package com.eclectics.chamapayments.wrappers.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContributionAnalyticsWrapper {
    private String name;
    private Integer amount;
}
