package com.eclectics.chamapoll.model;

import com.eclectics.chamapoll.model.constants.Status;
import com.eclectics.chamapoll.model.jpaAudit.Auditable;
import lombok.*;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "poll_candidates")
public class PollCandidates extends Auditable implements Comparable<PollCandidates>{
    private String firstName;
    private String lastName;
     @Enumerated(EnumType.STRING)
    private Status status;
    private int voteCounts;
    private Long positionId;
    private Long memberId;
    private Long pollId;
    private Long lastUpdatedBy;
    @ManyToOne
    private PollPositions pollPositions;

    @Override
    public int compareTo(PollCandidates pollCandidates) {
        return pollCandidates.getCreatedOn().compareTo(this.getCreatedOn());
    }
}
