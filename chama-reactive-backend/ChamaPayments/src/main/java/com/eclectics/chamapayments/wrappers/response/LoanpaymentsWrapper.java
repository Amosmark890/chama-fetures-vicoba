package com.eclectics.chamapayments.wrappers.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.util.Date;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class LoanpaymentsWrapper {
    private double amount;
    private double oldamount;
    private double newamount;
    private String receiptnumber;
    private long loandisbursedid;
    private String loanproductname;
    private String membername;
    private String memberphonenumber;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private Date appliedon;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private Date trxdate;
}
