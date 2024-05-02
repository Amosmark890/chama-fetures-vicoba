package com.eclectics.chamapayments.wrappers.response;

import lombok.*;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InvestmentWrapper {
    String name;
    double value;
    String description;
    String managerphone;
    String managername;

    long id;
    String groupname;
    long groupid;

}
