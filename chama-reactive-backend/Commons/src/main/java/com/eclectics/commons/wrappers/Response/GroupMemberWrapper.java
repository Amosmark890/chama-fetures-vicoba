package com.eclectics.commons.wrappers.Response;

import lombok.*;

import java.util.Date;

/**
 * @author Alex Maina
 * @created 08/12/2021
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupMemberWrapper {
    private long id;
    private boolean activemembership=true;
    private boolean isrequesttoleaveactedon=false;
    private boolean requesttoleavegroup=false;
    private String deactivationreason;
    private String title;
    private String permissions;
    private MemberWrapper members;
    private GroupWrapper group;
    private Date createdOn;
    private boolean softDelete;
}
