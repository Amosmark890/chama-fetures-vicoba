package com.eclectics.chamapayments.wrappers.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContributionWrapper {
    private long id;
    private long groupId;
    private String name;
    private String startDate;
    private long memberGroupId;
    private boolean active;
    private Integer reminder;
    private Double penalty;
    private Long contributionAmount;
    private Boolean ispercentage;
    private LocalDate dueDate;
    private String amountType;
    private String contributionTypeName;
    private String scheduleTypeName;
}
