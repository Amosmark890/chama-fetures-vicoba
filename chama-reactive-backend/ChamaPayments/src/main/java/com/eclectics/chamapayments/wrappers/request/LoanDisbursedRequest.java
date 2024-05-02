package com.eclectics.chamapayments.wrappers.request;

import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

@Getter
@Setter
public class LoanDisbursedRequest {
    int page;
    int size;
    String filter;
    Optional<Long> filterid;
}
