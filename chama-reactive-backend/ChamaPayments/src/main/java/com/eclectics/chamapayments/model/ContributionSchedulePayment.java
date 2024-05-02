package com.eclectics.chamapayments.model;

import com.eclectics.chamapayments.model.jpaAudit.Auditable;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Entity
@Table(name = "contribution_schedule_payment")
public class ContributionSchedulePayment extends Auditable {
    private Long  contributionId;
    @Column(unique = true)
    private String contributionScheduledId;
    private String expectedContributionDate;
}
