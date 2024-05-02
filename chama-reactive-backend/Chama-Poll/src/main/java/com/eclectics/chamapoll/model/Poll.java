package com.eclectics.chamapoll.model;

import com.eclectics.chamapoll.model.constants.Status;
import com.eclectics.chamapoll.model.jpaAudit.Auditable;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.Date;

/**
 * @author Alex Maina
 * @created 27/12/2021
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "poll")
public class Poll extends Auditable implements Comparable<Poll> {
    private String description;
    private Date registrationStart;
    private Date registrationEnd;
    private Date votingStart;
    @Enumerated(EnumType.STRING)
    private Status status;
    private boolean resultsApplied;
    private Date votingEnd;
    private long groupId;

    public Poll(String description, Date registrationStart, Date registrationEnd, Date votingStart, Status status, Date votingEnd, Long createdBy, Long groupId) {
        this.description = description;
        this.registrationStart = registrationStart;
        this.registrationEnd = registrationEnd;
        this.votingStart = votingStart;
        this.status = status;
        this.votingEnd = votingEnd;
        this.groupId = groupId;
        this.setCreatorId(createdBy);
        this.resultsApplied=false;
    }

    @Override
    public int compareTo(Poll poll) {
        return poll.getCreatedOn().compareTo(this.createdOn);
    }
}
