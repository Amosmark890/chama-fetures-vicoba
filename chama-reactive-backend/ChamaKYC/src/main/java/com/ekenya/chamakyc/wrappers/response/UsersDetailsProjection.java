package com.ekenya.chamakyc.wrappers.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public interface UsersDetailsProjection {
    long getId();
    String getFirstname();
    String getOthernames();
    String getPhonenumber();
    String getEmail();
    String getImsi();
    String getNationality();
    String getGender();
    String getIdentification();
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    Date getDateofbirth();
    double getWalletbalance();
    String getLinkedAccounts();
    boolean getActive();
    boolean getWalletexists();
}
