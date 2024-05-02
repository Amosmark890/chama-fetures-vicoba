package com.eclectics.chamapoll.model;

import com.eclectics.chamapoll.model.jpaAudit.Auditable;
import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "poll_votes_data")
public class PollVoteData extends Auditable {
    private  Long memberId;
    private  Long positionId;
    private  Long candidateId;
    private  LocalDateTime voteTime;
    @ManyToOne
    private PollPositions pollPositions;
    @ManyToOne
    private PollCandidates pollCandidates;
}
