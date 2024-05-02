package com.eclectics.chamapayments.model;

import com.eclectics.chamapayments.model.jpaAudit.Auditable;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Keeps track of the overpaid contributions made by a member in a group
 * and contribution. To be used when creating a penalty for a member.
 * If a member has an overpaid contribution, they don't get a
 * penalty if the value is >= to the upcoming contribution.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "overpaid_contributions")
public class OverpaidContribution extends Auditable {
    @Column(nullable = false)
    private Long groupId;
    @Column(nullable = false)
    private Double amount;
    @Column(nullable = false)
    private Long memberId;
    @Column(nullable = false)
    private String phoneNumber;
    @Column(nullable = true)
    private String lastEsbTransactionCode;
    @Column(nullable = false)
    private Long contributionId;
}
