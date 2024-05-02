package com.eclectics.chamapoll.wrappers;

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
    private long groupId;
    private long memberId;
    private boolean activemembership=true;
    private boolean isrequesttoleaveactedon=false;
    private boolean requesttoleavegroup=false;
    private String deactivationreason;
    private String title;
    private String groupName;
    private String phoneNumber;
    private String permissions;
    private Date createdOn;
    private boolean softDelete;
}
