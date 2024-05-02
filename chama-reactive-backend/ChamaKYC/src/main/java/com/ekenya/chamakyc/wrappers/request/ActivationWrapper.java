package com.ekenya.chamakyc.wrappers.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActivationWrapper {
    private String email;
    private String phoneNumber;
}
