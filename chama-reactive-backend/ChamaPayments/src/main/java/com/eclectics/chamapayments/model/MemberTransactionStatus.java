package com.eclectics.chamapayments.model;

import com.eclectics.chamapayments.model.jpaAudit.Auditable;
import com.eclectics.chamapayments.service.constants.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberTransactionStatus extends Auditable {
    private String walletAccount;
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
    private LocalDateTime lastTransacted;
}
