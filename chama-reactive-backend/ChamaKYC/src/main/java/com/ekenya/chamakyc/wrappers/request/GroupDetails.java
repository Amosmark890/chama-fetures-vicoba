package com.ekenya.chamakyc.wrappers.request;

import com.ekenya.chamakyc.wrappers.broker.TransactionslogsWrapper;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupDetails {
    private long groupid;
    private String groupname;
    private String location;
    private String description;
    private String createdby;
    @JsonFormat(pattern="dd-MM-yyyy HH:mm:ss")
    private Date createdon;
    private int size;
    private int totalInvites;
    private double walletBalance;
    private int totalContributions;
    private boolean walletExists;
    private double paybillMpesaContribution;
    private double groupLoans;
    private List<TransactionslogsWrapper> transactions;
    private Date nextContributionDate;
    private int contributionAmount;
    private double loans;
    private double  expenses;
    private double penalties;
    private String frequency;
    private int reminder;
    private double penalty;
    private boolean isPenaltyPercentage;
    private String cbsAccount;
    private List<GroupsDocumentsWrapper> groupFiles;
}
