package com.eclectics.chamapayments.model;

import com.eclectics.chamapayments.model.jpaAudit.Auditable;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@ToString
@Table(name = "contributions_tbl")
public class Contributions extends Auditable {
    private String name;
    private Date startDate;
    private String contributiondetails;
    private long memberGroupId;
    private boolean active;
    @Column(name = "reminder")
    private Integer reminder;
    @Column(name = "penalty")
    private Double penalty;
    @Column(name = "contribution_amount")
    private Long contributionAmount;
    @Column(name = "ispenaltypercentage")
    private Boolean ispercentage;
    @Column(name = "duedate")
    private LocalDate duedate;
    @ManyToOne
    @JoinColumn(name = "amount_type", nullable = false)
    @JsonProperty("amountType")
    private AmountType amountType;
    @ManyToOne(fetch = FetchType.EAGER)
    private ContributionType contributionType;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "schedule_type", nullable = false)
    @JsonProperty("scheduleType")
    private ScheduleTypes scheduleType;
}
