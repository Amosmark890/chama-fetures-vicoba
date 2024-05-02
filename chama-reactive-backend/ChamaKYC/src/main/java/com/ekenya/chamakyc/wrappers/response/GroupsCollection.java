package com.ekenya.chamakyc.wrappers.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Class name: GroupsCollection
 * Creater: wgicheru
 * Date:2/21/2020
 */
@Getter
@Setter
public class GroupsCollection {
    long groupid;
    String groupname;
    String location;
    String createdby;
    @JsonFormat(pattern="dd-MM-yyyy HH:mm:ss")
    Date createdon;
    int size;
}
