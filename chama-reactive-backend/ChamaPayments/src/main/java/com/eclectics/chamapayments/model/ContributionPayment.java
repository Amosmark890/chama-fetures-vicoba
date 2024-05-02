package com.eclectics.chamapayments.model;

import com.eclectics.chamapayments.model.jpaAudit.Auditable;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "contribution_payment")
public class ContributionPayment extends  Auditable {
    private long contributionId;
    private String transactionId;
    private String paymentStatus;
    private Integer amount;
    private String phoneNumber;
    private String mpesaPaymentId;
    private String paymentFailureReason;
    private String mpesaCheckoutId;
    private Long groupAccountId;
    private String paymentType;
    private Boolean isPenalty;
    private String receiptImageUrl;
    private Long penaltyId;
    private String schedulePaymentId;
    private Boolean isCombinedPayment;
}
