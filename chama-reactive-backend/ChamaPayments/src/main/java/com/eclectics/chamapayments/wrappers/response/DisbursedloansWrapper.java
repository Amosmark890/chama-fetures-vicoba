package com.eclectics.chamapayments.wrappers.response;

import com.eclectics.chamapayments.wrappers.request.AccountWrapper;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DisbursedloansWrapper {
    private long loanid;
    private String transactionid;
    private double principal;
    private double interest;
    private double dueamount;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private Date duedate;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private long daysoverdue;
    private String recipient;
    private String recipientsnumber;
    private long groupid;
    private String groupname;
    private long contributionid;
    private String contributionname;
    private String paymentperiodtype;
    private String loanproductname;
    private AccountWrapper debitAccount;
    private long accountTypeId;
    private boolean isGuarantor;
    private List<LoanPenaltyWrapper> loanPenaltyWrapperList;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private Date appliedon;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private Date approvedon;
}
