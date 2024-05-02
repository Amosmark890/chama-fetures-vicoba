package com.eclectics.chamapayments.model;

import com.eclectics.chamapayments.model.jpaAudit.Auditable;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


@Entity
@Table(name = "account_withdrawal_log")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WithdrawalLogs extends Auditable {
    private String uniqueTransactionId;
    private String contribution_narration;
    private String creditphonenumber;
    private double oldbalance;
    private double transamount;
    private double newbalance;
    private String capturedby;
    private  String withdrawalreason;
    private String transferToUserStatus;
    private long memberGroupId;
    @ManyToOne
    @JoinColumn(name = "debitaccount_id",referencedColumnName = "id",nullable = false)
    private Accounts debitAccounts;
    @ManyToOne
    @JoinColumn(name = "contribution_id",referencedColumnName = "id",nullable = false)
    private Contributions contributions;
    @ManyToOne
    @JoinColumn(name = "loan_application_id",referencedColumnName = "id",nullable = true)
    private LoanApplications loanApplications;
}
