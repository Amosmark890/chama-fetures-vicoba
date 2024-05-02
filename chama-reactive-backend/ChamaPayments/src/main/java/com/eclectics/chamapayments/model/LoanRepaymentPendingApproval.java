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
@Table(name = "loans_repayment_pendingapproval")
public class LoanRepaymentPendingApproval extends Auditable {
    private long memberId;
    private double amount;
    private String receiptnumber;
    private boolean pending;
    private boolean approved;
    private String approvedby;
    private String receiptImageUrl;
    private String paymentType;
    private String paymentId;
    private String mpesaCheckoutId;
    @ManyToOne
    @JoinColumn(name = "loandisbursed_id", nullable = false)
    private LoansDisbursed loansDisbursed;
}
