package com.eclectics.chamapayments.wrappers.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PendingWithdrawalsRequest {
    private Long groupId;
    private Integer page;
    private Integer size;
}
