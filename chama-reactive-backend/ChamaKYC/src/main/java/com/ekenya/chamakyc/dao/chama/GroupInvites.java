package com.ekenya.chamakyc.dao.chama;

import com.ekenya.chamakyc.dao.jpaAudit.Auditable;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "group_invites")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupInvites extends Auditable {
    //status can contain 3 values; active, accepted, rejected
    private String status;
    private String phonenumber;
    private boolean newmember;
    private String invitedrole;
    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;
}
