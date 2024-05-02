package com.ekenya.chamakyc.wrappers.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Class name: UsersDetails
 * Creater: wgicheru
 * Date:2/5/2020
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UsersDetails {

    @JsonIgnore
    @JsonProperty(value = "userId")
    long userid;
    String firstname;
    String othernames;
    String phonenumber;
    String email;
    String imsi;
    String nationality;
    String gender;
    String identification;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    String dateofbirth;
    double walletbalance;
    String linkedAccounts;
    boolean active;
    boolean walletexists;
    String profilePic;
    Object permissions;
    boolean blocked;
}
