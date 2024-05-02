package com.ekenya.chamakyc.wrappers.broker;

import lombok.*;

import java.util.Date;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ChamaContribution {
    private String contributionname;
    private Date startdate;
    private Map<String, Object> contributiondetails;
    private long contributiontypeid;
    private long scheduletypeid;
    private long groupid;
    private long amounttypeid;
    private Long penalty;
    private Integer reminders;
    private String createdby;
    private String initialAmount;
}
