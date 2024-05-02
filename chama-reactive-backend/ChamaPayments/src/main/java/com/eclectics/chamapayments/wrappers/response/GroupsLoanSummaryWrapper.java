package com.eclectics.chamapayments.wrappers.response;

import lombok.*;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class GroupsLoanSummaryWrapper {
    private String firstName;
    private String lastName;
    private String groupName;
    private String paymentType;
    private double amount;
    private Date  paymentDate;
}
