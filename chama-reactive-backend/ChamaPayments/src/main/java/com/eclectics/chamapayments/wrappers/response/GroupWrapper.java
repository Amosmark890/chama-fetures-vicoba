package com.eclectics.chamapayments.wrappers.response;

import lombok.*;

import java.util.Date;
import java.util.Set;

/**
 * @author Alex Maina
 * @created 08/12/2021
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class GroupWrapper  implements  Comparable<GroupWrapper>{
    private long id;
    private String name;
    private String location;
    private String description;
    private boolean active;
    private Long categoryId;
    private String groupImageUrl;
    private String purpose;
    private String groupConfig;
    private String csbAccount;
    private Date createdOn;
    private Date updateOn;
    private boolean isDeleted;
    private MemberWrapper creator;
    private Set<GroupMemberWrapper> groupmembers;
    private boolean walletexists;

    @Override
    public int compareTo(GroupWrapper wrapper) {
        return createdOn.compareTo(wrapper.getCreatedOn());
    }
}
