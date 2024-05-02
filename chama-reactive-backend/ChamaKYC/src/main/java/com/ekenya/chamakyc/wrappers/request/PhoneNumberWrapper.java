package com.ekenya.chamakyc.wrappers.request;

import lombok.*;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import static com.ekenya.chamakyc.util.StringConstantsUtil.PHONE_NUMBER_MATCH;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PhoneNumberWrapper {
    @Size(max = 12, message = "Phone number length can only be of length 12")
    @Pattern(regexp = PHONE_NUMBER_MATCH, message = "Phone number cannot contain special characters and letters")
    String phoneNumber;
}
