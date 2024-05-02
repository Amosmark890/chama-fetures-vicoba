package com.eclectics.chamapayments.model;

import com.eclectics.chamapayments.model.jpaAudit.Auditable;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Table(name = "groups_tbl")
@Entity
@Builder
public class Group extends Auditable {
    private String name;
    private String location;
    private String description;
    private double latitude;
    private double longitude;
    private boolean active=true;
    private Long categoryId;
    private String groupImageUrl;
    private String purpose;
    private String groupConfig;
    private String cbsAccount;
    private String cbsAccountName;
//    @ManyToOne
//    @JoinColumn(name = "creator", nullable = false)
//    @JsonProperty("creator")
//    private Member creator;
//
//    @JsonBackReference
//    @OneToMany(mappedBy = "group",fetch = FetchType.EAGER)
//    Set<GroupMembership> groupmembers;
//
//    @OneToMany(mappedBy = "group",fetch = FetchType.EAGER)
//    Set<GroupInvites> groupinvites;
//
//    @Column(columnDefinition = "boolean default false")
//    boolean walletexists;
}
