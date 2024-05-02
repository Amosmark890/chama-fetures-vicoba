package com.eclectics.chamapayments.model;

import com.eclectics.chamapayments.model.jpaAudit.Auditable;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "accounts")
public class Accounts extends Auditable {
    private String name;
    private String accountdetails;
    @ManyToOne()
    @JoinColumn(name = "accountType", nullable = false)
    @JsonMerge
    @JsonProperty("accountType")
    private AccountType accountType;
    private double accountbalance;
    private double availableBal;
    private long groupId;
    @Column(name = "is_active")
    private boolean active;
}
