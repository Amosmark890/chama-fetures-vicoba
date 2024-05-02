package com.eclectics.chamapayments.model;

import com.eclectics.chamapayments.model.jpaAudit.Auditable;
import lombok.*;

import javax.persistence.Entity;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Entity
@Builder
public class Investments extends Auditable {
    private String name;
    private long groupId;
    private double value;
    private String description;
    private String managerphone;
    private String managername;
}
