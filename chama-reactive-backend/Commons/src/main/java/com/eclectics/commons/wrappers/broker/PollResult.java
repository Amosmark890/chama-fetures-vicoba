package com.eclectics.commons.wrappers.broker;

import lombok.*;

/**
 * @author Alex Maina
 * @created 29/12/2021
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PollResult {
    private String postName;
    private long groupId;
    private long memberId;
}
