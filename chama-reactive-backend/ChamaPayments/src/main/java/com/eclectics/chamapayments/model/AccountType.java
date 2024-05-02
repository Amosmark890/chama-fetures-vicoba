package com.eclectics.chamapayments.model;

import com.eclectics.chamapayments.model.jpaAudit.Auditable;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "account_types")
public class AccountType extends Auditable {
    private String accountName;
    private String accountPrefix;
    private String accountFields;
}
