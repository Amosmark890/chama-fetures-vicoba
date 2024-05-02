package com.eclectics.commons.wrappers.Response;

import com.eclectics.commons.wrappers.Response.TransactionslogsWrapper;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@NoArgsConstructor
@Data
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
