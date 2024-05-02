package com.eclectics.chamapayments.model;

import com.eclectics.chamapayments.model.jpaAudit.Auditable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

/**
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_loan_penalty_tbl")
public class LoanPenalty extends Auditable {
    private long memberId;
    private Double penaltyAmount;
    private String paymentStatus;
    private Double paidAmount;
    private Double dueAmount;
    private String transactionId;
    private String receiptNumber;
    private String paymentMethod;
    private Date lastPaymentDate;
    private Date loanDueDate;
    private String paymentPeriod;
    private String expectedPaymentDate;
    @ManyToOne
    @JoinColumn(name = "loandisbursed_id", nullable = false,referencedColumnName = "id")
    private LoansDisbursed loansDisbursed;


}
