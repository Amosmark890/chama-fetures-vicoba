package com.ekenya.chamakyc.wrappers.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GroupMembershipWrapper {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String name;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String email;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String phonenumber;
    private long groupid;
    private String groupname;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String location;
    private int groupsize;
    private boolean activemembership;
    private String title;
    @JsonFormat(pattern="dd-MM-yyyy")
    private Date createdon;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String deactivationreason;
    private long id;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String groupImageUrl;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private boolean approve;
    private boolean isGroupActive;
}
