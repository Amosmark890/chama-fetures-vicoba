package com.eclectics.chamapayments.wrappers.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.util.Date;

/**
 * @author Alex Maina
 * @created 08/12/2021
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContributionSchedulePaymentWrapper {
    private long schedulePaymentId;
    private String groupName;
    private long contributionPaymentId;
    private String contributionName;
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private Date contributionStartDate;
    private String contributionType;
    private String scheduleType;
    private String expectedContributionDate;
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private Date createdOn;
    private String scheduledId;
}
