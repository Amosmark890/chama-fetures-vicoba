package com.eclectics.chamapayments.wrappers.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WithdrawalApprovalRequest {
    private Long groupId;
    private Long requestId;
    private Boolean approve;
}
