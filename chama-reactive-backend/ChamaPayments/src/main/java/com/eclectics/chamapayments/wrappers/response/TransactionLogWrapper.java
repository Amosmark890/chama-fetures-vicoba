package com.eclectics.chamapayments.wrappers.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.util.Date;

/**
 * @author Alex Maina
 * @created 06/12/2021
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionLogWrapper implements Comparable<TransactionLogWrapper> {
    private String transactionid;
    private String narration;
    private String debitaccount;
    private String creditaccount;
    private String creditaccountname;
    private String accounttype;
    private double amount;
    private long contributionid;
    private String contributionname;
    private String groupname;
    private String membername;
    private String capturedby;
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private Date transactiondate;

    @Override
    public int compareTo(TransactionLogWrapper log) {
        if(transactiondate == null || log.transactiondate == null)
            return 0;

        return transactiondate.compareTo(log.transactiondate);
    }
}
