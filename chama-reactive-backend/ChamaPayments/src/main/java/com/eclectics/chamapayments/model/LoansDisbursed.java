package com.eclectics.chamapayments.model;

import com.eclectics.chamapayments.model.jpaAudit.Auditable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.Date;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loans_disbursed")
public class LoansDisbursed extends Auditable {
    private double principal;
    private double interest;
    private Date duedate;
    private Date paymentStartDate;
    private double dueamount;
    @OneToOne
    @JoinColumn(name="loanapplicationid",referencedColumnName = "id")
    private LoanApplications loanApplications;
    @OneToOne
    @JoinColumn(name="withdrawal_logid",referencedColumnName = "id")
    private WithdrawalLogs withdrawalLogs;
    private long groupId;
    private long memberId;
    private String paymentPeriodType;
    private String status;
}
