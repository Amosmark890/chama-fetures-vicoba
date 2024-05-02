package com.eclectics.chamapayments.model;

import com.eclectics.chamapayments.model.jpaAudit.Auditable;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "transactions_pending_approval")
public class TransactionsPendingApproval extends Auditable {
    private String phonenumber;
    private double amount;
    private String capturedby;
    private String contribution_narration;
    private boolean pending;
    private boolean approved;
    private String approvedby;
    private Long contributionPaymentId;
    @ManyToOne
    @JoinColumn(name = "contribution_id",referencedColumnName = "id",nullable = false)
    private Contributions contribution;
    @ManyToOne
    @JoinColumn(name = "creditaccount_id",referencedColumnName = "id",nullable = false)
    private Accounts account;
}
