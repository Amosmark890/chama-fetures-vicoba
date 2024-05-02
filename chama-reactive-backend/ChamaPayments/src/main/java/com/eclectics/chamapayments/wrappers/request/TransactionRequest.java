package com.eclectics.chamapayments.wrappers.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

@Getter
@Setter
@AllArgsConstructor
public class TransactionRequest {
    Integer page;
    Integer size;
    String filter;
    Optional<Long> filterId;
}
