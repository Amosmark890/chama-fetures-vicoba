package com.eclectics.chamapoll.wrappers;


import lombok.*;

import java.util.Date;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberWrapper implements Comparable<MemberWrapper> {
    private long id;
    private String firstname;
    private String lastname;
    private Date dateofbirth;
    private String phonenumber;
    private String countrycode;
    private String identification;
    private String nationality;
    private String gender;
    private String userDeviceId;
    private boolean active;
    private boolean isregisteredmember;
    private String email;
    private boolean ussdplatform;
    private String imsi;
    private boolean androidplatform;
    private boolean iosplatform;
    private Date lastlogin;
    private boolean deactivated;
    private Date deactivationdate;
    private String esbwalletaccount;
    private boolean walletexists;
    private boolean isFirstTimeLogin;
    private Date createdOn;
    private Date lastUpdatedOn;
    private boolean softDelete;
    private String language;

    @Override
    public int compareTo(MemberWrapper wrapper) {
        return createdOn.compareTo(wrapper.getCreatedOn());
    }
}
