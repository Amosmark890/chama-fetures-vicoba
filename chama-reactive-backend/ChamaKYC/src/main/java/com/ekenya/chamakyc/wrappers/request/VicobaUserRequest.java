package com.ekenya.chamakyc.wrappers.request;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VicobaUserRequest {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String identification;
    private String nationality;
    private String gender;
    private String accountType;
    private String middleName;
    private String language;
    private String email;
    private String linkedAccounts;
}
