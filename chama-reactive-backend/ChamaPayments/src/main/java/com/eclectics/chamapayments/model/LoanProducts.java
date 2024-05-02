package com.eclectics.chamapayments.model;

import com.eclectics.chamapayments.model.jpaAudit.Auditable;
import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.*;

import javax.persistence.*;
import java.util.Set;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "loan_products")
public class LoanProducts extends Auditable {
    private String productname;
    private String description;
    private double max_principal;
    private double min_principal;
    private String interesttype;
    private double interestvalue;
    private String paymentperiodtype;
    private int paymentperiod;
    private long  groupId;
    private boolean isGuarantor;
    private Integer gracePeriod;
    private boolean isPenalty;
    private Integer penaltyValue;
    private Boolean isPercentagePercentage;
    private int userSavingValue;
    @Column(name = "is_active")
    private Boolean isActive;
    @Column(name = "penalty_period")
    private String penaltyPeriod;
    @OneToMany(mappedBy = "loanProducts", fetch = FetchType.EAGER)
    @JsonBackReference
    private Set<LoanApplications> loanApplications;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "debit_account_id", nullable = false)
    private Accounts debitAccountId;
    @ManyToOne
    @JoinColumn(name = "contributionid", referencedColumnName = "id")
    private Contributions contributions;
}
