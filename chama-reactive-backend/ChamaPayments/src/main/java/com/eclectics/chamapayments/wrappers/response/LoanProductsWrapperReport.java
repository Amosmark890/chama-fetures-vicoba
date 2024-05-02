package com.eclectics.chamapayments.wrappers.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.util.Date;

/**
 * @author Alex Maina
 * @created 08/12/2021
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class LoanProductsWrapperReport {
    private long productid;
    private String productname;
    private String description;
    private double max_principal;
    private double min_principal;
    private String interesttype;
    private double interestvalue;
    private int paymentperiod;
    private String paymentperiodtype;
    private long contributionid;
    private String contributionname;
    private double contributionbalance;
    private String groupname;
    private long groupid;
    private boolean isguarantor;
    private boolean ispenalty;
    private int penaltyvalue;
    private boolean ispenaltypercentage;
    private int usersavingvalue;
    private double userLoanLimit;
    private long debitAccountId;
    private boolean isActive;
    private String penaltyPeriod;
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private Date createdOn;
}
