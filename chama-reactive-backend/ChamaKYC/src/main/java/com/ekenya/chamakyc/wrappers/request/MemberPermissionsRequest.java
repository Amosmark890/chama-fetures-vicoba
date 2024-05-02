package com.ekenya.chamakyc.wrappers.request;

import lombok.*;

import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberPermissionsRequest {
    private Long groupId;
    private Optional<String> phoneNumber;
}
