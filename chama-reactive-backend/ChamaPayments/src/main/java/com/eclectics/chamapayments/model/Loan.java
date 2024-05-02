package com.eclectics.chamapayments.model;

import com.eclectics.chamapayments.model.jpaAudit.Auditable;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Date;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "contribution_loan_tbl")
public class Loan extends Auditable {
    private long contributionId;
    private String phoneNumber;
    private double amount;
    private Date repaymentDate;
    private String loanStatus;
    private float interestRate;
    private String loanType;
    private String transactionStatus;
    private String transactionRef;
    private String transactionDescription;

    @OneToMany
    private List<Guarantors> guarantors;
}
