package com.eclectics.chamapayments.wrappers.request;

import com.eclectics.chamapayments.model.AccountDetails;
import com.eclectics.chamapayments.model.AccountType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountDto {
    private long accountId;
    private String name;
    private long groupId;
    private boolean active;
    private double availableBal;
    private double accountbalance;
    private AccountDetails accountdetails;
    private AccountType accountType;
}
