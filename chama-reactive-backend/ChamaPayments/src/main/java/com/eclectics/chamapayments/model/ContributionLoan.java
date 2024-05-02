package com.eclectics.chamapayments.model;

import com.eclectics.chamapayments.model.jpaAudit.Auditable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "contribution_loan_tbl")
public class ContributionLoan extends Auditable {
    private Long contributionId;
    private String phoneNumber;
    private Integer amount;
    private Date repaymentDate;
    private String loanStatus;
    private Float interestRate;
    private String loanType;
    private String transactionStatus;
    private String transactionRef;
    private String transactionDescription;

    @Transient
    private List<Guarantors> guarantors;
}
