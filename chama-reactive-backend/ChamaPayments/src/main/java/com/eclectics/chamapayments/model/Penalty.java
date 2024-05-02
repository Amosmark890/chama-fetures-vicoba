package com.eclectics.chamapayments.model;

import com.eclectics.chamapayments.model.jpaAudit.Auditable;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_penalty_tbl")
public class Penalty extends Auditable {
    private long userId;
    private boolean isPaid;
    private String schedulePaymentId;
    private String paymentPhoneNumber;
    private String contributionName;
    private String paymentStatus;
    private String expectedPaymentDate;
    private long contributionId;
    private long groupId;
    private double amount;
    private String transactionId;
    private LocalDate defaultDate;
}
