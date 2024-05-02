package com.eclectics.chamapoll.model;

import com.eclectics.chamapoll.model.jpaAudit.Auditable;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Builder
@ToString
public class PollPositions extends Auditable {
    private String name;
    private String description;
    private boolean status;
    private int totalCandidates;
    private int totalVotesCasted;
    @ManyToOne
    private Poll poll;
}
