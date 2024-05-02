package com.eclectics.chamapayments.model;

import com.eclectics.chamapayments.model.jpaAudit.Auditable;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Builder
@Table(name = "loans_repayment")
public class LoansRepayment extends Auditable {
    private double amount;
    private double oldamount;
    private double newamount;
    private String receiptnumber;
    private String paymentType;
    private long memberId;
    private String status;
    @ManyToOne
    @JoinColumn(name = "loandisbursed_id", nullable = false)
    private LoansDisbursed loansDisbursed;

}
