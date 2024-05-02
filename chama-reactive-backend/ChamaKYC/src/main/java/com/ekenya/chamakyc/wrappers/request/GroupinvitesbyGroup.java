package com.ekenya.chamakyc.wrappers.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Class name: GroupinvitesbyGroup
 * Creater: wgicheru
 * Date:2/26/2020
 */
@Getter
@Setter
public class GroupinvitesbyGroup {
    long inviteid;
    String phonenumber;
    String status;
    boolean registeredmember;
    @JsonFormat(pattern="dd-MM-yyyy HH:mm:ss")
    Date createdon;
}
