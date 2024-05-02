package com.ekenya.chamakyc.wrappers.request;

import lombok.*;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import static com.ekenya.chamakyc.util.StringConstantsUtil.CBS_ACCOUNT_MATCH;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountNumberWrapper {
    @NonNull
    @Size(max = 15, message = "Account number can only have a max of 15 digits")
    @Pattern(regexp = CBS_ACCOUNT_MATCH, message = "Account number cannot contain letters or special characters")
    String account;
}
