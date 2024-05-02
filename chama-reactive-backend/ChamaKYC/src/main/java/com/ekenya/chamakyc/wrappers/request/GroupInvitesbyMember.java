package com.ekenya.chamakyc.wrappers.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Class name: GroupInvitesbyMember
 * Creater: wgicheru
 * Date:2/21/2020
 */
@Getter
@Setter
public class GroupInvitesbyMember {
    long id;
    String groupname;
    @JsonFormat(pattern="dd-MM-yyyy HH:mm:ss")
    Date createdon;
}
