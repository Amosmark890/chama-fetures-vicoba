package com.eclectics.chamapayments.model;

import com.eclectics.chamapayments.model.jpaAudit.Auditable;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Entity
public class Guarantors extends Auditable {
    private String loanStatus;
    private double amount;
    private String phoneNumber;
    private Long loanId;
    @Transient
    private String guarantorName;
    @Transient
    private int status;
}
