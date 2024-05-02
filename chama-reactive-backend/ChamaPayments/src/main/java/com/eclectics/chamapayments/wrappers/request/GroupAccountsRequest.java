package com.eclectics.chamapayments.wrappers.request;

import lombok.*;

import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupAccountsRequest {
    private Long groupId;
    private Optional<Long> accountypeId;
}
