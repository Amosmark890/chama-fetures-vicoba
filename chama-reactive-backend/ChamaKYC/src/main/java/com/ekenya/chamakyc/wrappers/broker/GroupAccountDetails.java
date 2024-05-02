package com.ekenya.chamakyc.wrappers.broker;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class GroupAccountDetails {
    private double walletBalance;
    private int contributionCount;
    private double paybillMpesaContributions;
    private double groupLoans;
    private boolean isPenaltyPercentage;
    private double  contributionPenalty;
    private double contributionAmount;
    private Date nextContributionDate;
    private List<TransactionslogsWrapper> transactionsLogs;
    private double totalGroupPenalties;
    private double groupExpenses;
    private int groupReminder;
    private double groupWithdrawalsTotal;
    private String frequency;

}
