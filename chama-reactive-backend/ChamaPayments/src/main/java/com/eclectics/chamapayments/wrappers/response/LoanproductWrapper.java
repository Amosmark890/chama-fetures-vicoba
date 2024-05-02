package com.eclectics.chamapayments.wrappers.response;


import lombok.*;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class LoanproductWrapper {
    private long productid;
    @NotNull(message = "name cannot be null") @NotEmpty(message = "name cannot be empty")
    private String productname;
    private String description;
    @NotNull(message = "max_principal cannot be null")
    private double max_principal;
    @NotNull(message = "min_principal cannot be null")
    private double min_principal;
    @NotNull(message = "interesttype cannot be null") @NotEmpty(message = "interesttype cannot be empty")
    private String interesttype;
    @NotNull(message = "interestvalue cannot be null")
    private double interestvalue;
    @NotNull(message = "paymentperiod cannot be null")
    private int paymentperiod;
    @NotNull(message = "paymentperiodtype cannot be null")
    private String paymentperiodtype;
    @NotNull(message = "contributionid cannot be null")
    private long contributionid;
    private String contributionname;
    private double contributionbalance;
    private String groupname;
    @NotNull(message = "groupid cannot be null")
    private long groupid;
    private Boolean isguarantor;
    private Boolean hasPenalty;
    private Integer penaltyvalue;
    private Boolean ispenaltypercentage;
    private Integer usersavingvalue = 100;
    private Double userLoanLimit;
    private Long debitAccountId;
    private Boolean isActive;
    private String penaltyPeriod;
    private Integer gracePeriod = 1;
}
