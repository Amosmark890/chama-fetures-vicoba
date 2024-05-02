package com.eclectics.chamapayments.model;

import com.eclectics.chamapayments.model.jpaAudit.Auditable;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.*;

import javax.persistence.*;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Getter
@Setter
@ToString
@Table(name = "loan_applications")
public class LoanApplications extends Auditable {
    private long memberId;
    private double amount;
    private String reminder;
    private int unpaidloans;
    private boolean pending;
    private boolean approved;
    private String approvedby;
    private String status;
    private int approvalCount;
    private boolean isUsingWallet;
    private String accountToDeposit;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="loanproductid",referencedColumnName = "id")
    @JsonManagedReference
    private LoanProducts loanProducts;
}
