package com.eclectics.chamapayments.wrappers.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.util.Date;
import java.util.Map;

/**
 * Class name: LoansPendingApprovalWrapper
 * Date:4/22/2020
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoansPendingApprovalWrapper {
    private long loanproductid;
    private long loanapplicationid;
    private double amount;
    private String loanproductname;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private Date appliedon;
    private String membername;
    private String memberphonenumber;
    private int unpaidloans;
    private Map<String, Object> reminder;
    private boolean isGuarantor;
}
