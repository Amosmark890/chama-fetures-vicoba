package com.eclectics.chamapayments.wrappers.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BalanceInquiry {
    private String availableBal;
    private String actualBal;
}
