package com.eclectics.chamapayments.model.jpaInterfaces;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface AccountsTotals {
    @JsonProperty(value = "accountBalances")
    public int getGroupbalances();
    @JsonProperty(value = "cbsAccountBalances")
    public int getCoreaccountbalances();
}
