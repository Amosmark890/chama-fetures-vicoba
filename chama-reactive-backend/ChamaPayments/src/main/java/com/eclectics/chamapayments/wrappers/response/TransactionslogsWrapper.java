package com.eclectics.chamapayments.wrappers.response;

import lombok.*;

import java.util.Date;

/**
 * Class name: TransactionslogsWrapper
 * Creater: wgicheru
 * Date:3/24/2020
 */
@Getter
@Setter
@AllArgsConstructor
@Builder
public class TransactionslogsWrapper implements Comparable<TransactionslogsWrapper> {
    Long id;
    String transactionid;
    String narration;
    String debitaccount;
    String creditaccount;
    String creditaccountname;
    String accounttype;
    double amount;
    long contributionid;
    String contributionname;
    String groupname;
    String membername;
    String capturedby;
    Date transactiondate;

    @Override
    public int compareTo(TransactionslogsWrapper transactionslogsWrapper) {
        if(transactiondate == null || transactionslogsWrapper.transactiondate == null)
             return 0;

        return transactiondate.compareTo(transactionslogsWrapper.transactiondate);
    }
}
