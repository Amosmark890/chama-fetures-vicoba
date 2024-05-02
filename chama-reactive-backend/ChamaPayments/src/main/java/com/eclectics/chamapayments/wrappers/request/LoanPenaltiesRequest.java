package com.eclectics.chamapayments.wrappers.request;

import lombok.*;

import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoanPenaltiesRequest {
    private String filter;
    private Optional<Long> filterId;
}
