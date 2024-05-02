package com.eclectics.chamapayments.wrappers.response;


import com.eclectics.chamapayments.model.Guarantors;
import com.eclectics.chamapayments.model.LoanApplications;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GuarantorLoansWrapper {
    private List<Guarantors> guarantorsList;
    private Double totalLoanAmount;
    private Double totalApprovedAmount;
    private Double totalDeclinedAmount;
    private Double totalPendingApprovalAmount;
    private Double remainingAmount;
    private LoanApplications loanApplications;
}
