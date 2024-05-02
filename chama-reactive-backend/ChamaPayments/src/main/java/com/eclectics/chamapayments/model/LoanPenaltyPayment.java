package com.eclectics.chamapayments.model;

import com.eclectics.chamapayments.model.jpaAudit.Auditable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loan_penalty_payment")
public class LoanPenaltyPayment extends Auditable {
    private String transactionId;
    private String receiptNumber;
    private String paymentMethod;
    private Double paidAmount;
    private String paymentStatus;
    private String receiptImageUrl;
    private String mpesaCheckoutId;
    @ManyToOne
    @JoinColumn(name="penalty_loan_id",referencedColumnName = "id")
    private LoanPenalty loanPenalty;
}
