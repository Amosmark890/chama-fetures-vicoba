package com.eclectics.chamapayments.wrappers.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.util.Date;

/**
 * @author Alex Maina
 * @created 08/12/2021
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class GroupLoansApprovedWrapper {
    private long loanproductid;
    private long loanapplicationid;
    private double amount;
    private String loanproductname;
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private Date appliedon;
    private String membername;
    private String memberphonenumber;
    private int unpaidloans;
    private String  reminder;
    private boolean isGuarantor;
    private String approvedBy;
    private String status;
}
