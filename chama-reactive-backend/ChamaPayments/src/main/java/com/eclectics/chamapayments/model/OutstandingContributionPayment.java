package com.eclectics.chamapayments.model;

import com.eclectics.chamapayments.model.jpaAudit.Auditable;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "outstanding_contribution_payment")
public class OutstandingContributionPayment extends Auditable {
    private long contributionId;
    private Integer dueAmount;
    private Integer paidAmount;
    @Column(unique = true)
    private Long memberId;
    private String schedulePaymentId;
}
