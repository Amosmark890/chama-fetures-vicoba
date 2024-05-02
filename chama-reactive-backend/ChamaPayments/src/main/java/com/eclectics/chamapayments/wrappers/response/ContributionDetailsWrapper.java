package com.eclectics.chamapayments.wrappers.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Map;

/**
 * Class name: ContributiondetailsWrapper
 * Creater: wgicheru
 * Date:3/23/2020
 */
@Getter
@Setter
public class ContributionDetailsWrapper {
    private long id;
    private String name;
    @JsonFormat(pattern="dd-MM-yyyy HH:mm:ss")
    private Date startdate;
    private Map<String,Object> contributiondetails;
    private long contributiontypeid;
    private String enddate;
    private String contributiontypename;
    private long scheduletypeid;
    private String scheduletypename;
    private long amounttypeid;
    private String amounttypename;
    private long groupid;
    private String groupname;
    private long amountcontributed;
    private boolean active;
    private String createdby;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private String dueDate;
    private String contributioncount;
    private Double penalty;
    private Boolean isPercentage;
}
