package com.eclectics.chamapayments.wrappers.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserGroupContributions {
    private String groupName;
    private Integer amount;
}
