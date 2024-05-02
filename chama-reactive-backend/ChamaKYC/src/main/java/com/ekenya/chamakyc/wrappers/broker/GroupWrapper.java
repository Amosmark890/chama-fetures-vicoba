package com.ekenya.chamakyc.wrappers.broker;

import lombok.*;

import java.io.Serializable;
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
public class GroupWrapper implements Comparable<GroupWrapper>, Serializable {
    private long id;
    private String name;
    private String location;
    private String description;
    private boolean active;
    private Long categoryId;
    private String groupImageUrl;
    private String purpose;
    private String csbAccount;
    private String groupConfig;
    private Date createdOn;
    private Date updateOn;
    private boolean isDeleted;
    private boolean walletexists;
    private String groupType;

    @Override
    public int compareTo(GroupWrapper wrapper) {
        return createdOn.compareTo(wrapper.getCreatedOn());
    }
}
