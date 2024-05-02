package com.eclectics.chamapayments.wrappers.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoanProductRequest {
    private Long id;
    private Long groupId;
}
