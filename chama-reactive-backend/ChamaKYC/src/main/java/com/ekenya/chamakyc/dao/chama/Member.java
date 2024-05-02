package com.ekenya.chamakyc.dao.chama;

import com.ekenya.chamakyc.dao.jpaAudit.Auditable;
import com.ekenya.chamakyc.dao.user.Users;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Builder
@Entity
@Table(name = "members_tbl")
public class Member extends Auditable {
    @Column(unique = true)
    private String imsi;
    private String userDeviceId;
    @Column(columnDefinition = "boolean default false")
    private boolean isregisteredmember;
    @Column(columnDefinition = "boolean default false")
    private boolean ussdplatform;
    @Column(columnDefinition = "boolean default false")
    private boolean androidplatform;
    @Column(columnDefinition = "boolean default false")
    private boolean iosplatform;
    private boolean active;
    private Date deactivationdate;
    @Column(unique = true)
    private String esbwalletaccount;
    @Column(columnDefinition = "boolean default false")
    private boolean walletexists;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String linkedAccounts;

    @OneToOne
    @JoinColumn(name = "user_id")
    private Users users;

    @JsonBackReference
    @OneToMany(mappedBy = "members",fetch = FetchType.EAGER)
    private Set<GroupMembership> groupmembership;

}
