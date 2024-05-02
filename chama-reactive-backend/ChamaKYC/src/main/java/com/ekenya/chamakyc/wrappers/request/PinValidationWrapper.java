package com.ekenya.chamakyc.wrappers.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PinValidationWrapper {
    String pass;
    String phoneNumber;
}
