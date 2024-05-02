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
    private String csbAccount;
    private String groupConfig;
    private Date createdOn;
    private Date updateOn;
    private boolean isDeleted;
    private boolean walletexists;

    @Override
    public int compareTo(GroupWrapper wrapper) {
        return createdOn.compareTo(wrapper.getCreatedOn());
    }
}
