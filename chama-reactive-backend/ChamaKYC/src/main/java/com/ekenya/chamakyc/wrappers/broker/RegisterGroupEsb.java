package com.ekenya.chamakyc.wrappers.broker;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RegisterGroupEsb {
    private Long groupId;
    private String groupName;
}
