package com.ekenya.chamakyc.wrappers.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OtpRequestWrapper {
    private String type;
    private String phone;
}
